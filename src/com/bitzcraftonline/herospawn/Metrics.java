package com.bitzcraftonline.herospawn;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlConfigurationOptions;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitScheduler;

public class Metrics
{
  private static final int REVISION = 5;
  private static final String BASE_URL = "http://metrics.griefcraft.com";
  private static final String REPORT_URL = "/report/%s";
  private static final String CONFIG_FILE = "plugins/PluginMetrics/config.yml";
  private static final String CUSTOM_DATA_SEPARATOR = "~~";
  private static final int PING_INTERVAL = 10;
  private Map<Plugin, Set<Graph>> graphs = Collections.synchronizedMap(new HashMap());

  private Map<Plugin, Graph> defaultGraphs = Collections.synchronizedMap(new HashMap());
  private final YamlConfiguration configuration;
  private String guid;

  public Metrics()
    throws IOException
  {
    File file = new File("plugins/PluginMetrics/config.yml");
    this.configuration = YamlConfiguration.loadConfiguration(file);

    this.configuration.addDefault("opt-out", Boolean.valueOf(false));
    this.configuration.addDefault("guid", UUID.randomUUID().toString());

    if (this.configuration.get("guid", null) == null) {
      this.configuration.options().header("http://metrics.griefcraft.com").copyDefaults(true);
      this.configuration.save(file);
    }

    this.guid = this.configuration.getString("guid");
  }

  public Graph createGraph(Plugin plugin, Metrics.Graph.Type type, String name)
  {
    if ((plugin == null) || (type == null) || (name == null)) {
      throw new IllegalArgumentException("All arguments must not be null");
    }

    Graph graph = new Graph(type, name);

    Set graphs = getOrCreateGraphs(plugin);

    graphs.add(graph);

    return graph;
  }

  public void addCustomData(Plugin plugin, Plotter plotter)
  {
    Graph graph = getOrCreateDefaultGraph(plugin);

    graph.addPlotter(plotter);

    getOrCreateGraphs(plugin).add(graph);
  }

  public void beginMeasuringPlugin(final Plugin plugin)
  {
    if (this.configuration.getBoolean("opt-out", false)) {
      return;
    }

    plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, new Runnable() {
      private boolean firstPost = true;

      public void run()
      {
        try
        {
          Metrics.this.postPlugin(plugin, !this.firstPost);

          this.firstPost = false;
        } catch (IOException e) {
          System.out.println("[Metrics] " + e.getMessage());
        }
      }
    }
    , 0L, 12000L);
  }

  @SuppressWarnings("rawtypes")
private void postPlugin(Plugin plugin, boolean isPing)
    throws IOException
  {
    PluginDescriptionFile description = plugin.getDescription();

    String data = encode("guid") + '=' + encode(this.guid) + 
      encodeDataPair("version", description.getVersion()) + 
      encodeDataPair("server", Bukkit.getVersion()) + 
      encodeDataPair("players", Integer.toString(Bukkit.getServer().getOnlinePlayers().length)) + 
      encodeDataPair("revision", String.valueOf(5));

    if (isPing) {
      data = data + encodeDataPair("ping", "true");
    }

    @SuppressWarnings("rawtypes")
	Set graphs = getOrCreateGraphs(plugin);

    synchronized (graphs) {
      Iterator iter = graphs.iterator();
      Iterator localIterator1;
      for (; iter.hasNext(); 
        localIterator1.hasNext())
      {
        Graph graph = (Graph)iter.next();

        localIterator1 = graph.getPlotters().iterator(); continue;
      }

    }

    URL url = new URL("http://metrics.griefcraft.com" + String.format("/report/%s", new Object[] { plugin.getDescription().getName() }));
    URLConnection connection;
    if (isMineshafterPresent())
      connection = url.openConnection(Proxy.NO_PROXY);
    else {
      connection = url.openConnection();
    }

    connection.setDoOutput(true);

    OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
    writer.write(data);
    writer.flush();

    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    String response = reader.readLine();

    writer.close();
    reader.close();

    if (response.startsWith("ERR")) {
      throw new IOException(response);
    }

    if (response.contains("OK This is your first update this hour"))
      synchronized (graphs) {
        Iterator iter = graphs.iterator();
        Iterator localIterator2;
        for (; iter.hasNext(); 
          localIterator2.hasNext())
        {
          Graph graph = (Graph)iter.next();

          localIterator2 = graph.getPlotters().iterator(); continue;
        }
      }
  }

  private Set<Graph> getOrCreateGraphs(Plugin plugin)
  {
    Set theGraphs = (Set)this.graphs.get(plugin);

    if (theGraphs == null) {
      theGraphs = Collections.synchronizedSet(new HashSet());
      this.graphs.put(plugin, theGraphs);
    }

    return theGraphs;
  }

  private Graph getOrCreateDefaultGraph(Plugin plugin)
  {
    Graph graph = (Graph)this.defaultGraphs.get(plugin);

    if (graph == null) {
      graph = new Graph(Metrics.Graph.Type.Line, "Default");
      this.defaultGraphs.put(plugin, graph);
    }

    return graph;
  }

  private boolean isMineshafterPresent()
  {
    try
    {
      Class.forName("mineshafter.MineServer");
      return true; } catch (Exception e) {
    }
    return false;
  }

  private static String encodeDataPair(String key, String value)
    throws UnsupportedEncodingException
  {
    return '&' + encode(key) + '=' + encode(value);
  }

  private static String encode(String text)
    throws UnsupportedEncodingException
  {
    return URLEncoder.encode(text, "UTF-8");
  }

  public static class Graph
  {
    private final Type type;
    private final String name;
    private final Set<Metrics.Plotter> plotters = new LinkedHashSet();

    private Graph(Type type, String name) {
      this.type = type;
      this.name = name;
    }

    public String getName()
    {
      return this.name;
    }

    public void addPlotter(Metrics.Plotter plotter)
    {
      this.plotters.add(plotter);
    }

    public void removePlotter(Metrics.Plotter plotter)
    {
      this.plotters.remove(plotter);
    }

    public Set<Metrics.Plotter> getPlotters()
    {
      return Collections.unmodifiableSet(this.plotters);
    }

    public int hashCode()
    {
      return this.type.hashCode() * 17 ^ this.name.hashCode();
    }

    public boolean equals(Object object)
    {
      if (!(object instanceof Graph)) {
        return false;
      }

      Graph graph = (Graph)object;
      return (graph.type == this.type) && (graph.name.equals(this.name));
    }

    public static enum Type
    {
      Line, 

      Area, 

      Column, 

      Pie;
    }
  }

  public static abstract class Plotter
  {
    private final String name;

    public Plotter()
    {
      this("Default");
    }

    public Plotter(String name)
    {
      this.name = name;
    }

    public abstract int getValue();

    public String getColumnName()
    {
      return this.name;
    }

    public void reset()
    {
    }

    public int hashCode()
    {
      return getColumnName().hashCode() + getValue();
    }

    public boolean equals(Object object)
    {
      if (!(object instanceof Plotter)) {
        return false;
      }

      Plotter plotter = (Plotter)object;
      return (plotter.name.equals(this.name)) && (plotter.getValue() == getValue());
    }
  }
}