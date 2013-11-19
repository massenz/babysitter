package com.rivermeadow.babysitter.alerts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xeustechnologies.jcl.JarClassLoader;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * TODO: enter class description here
 *
 * @author marco
 *
 */
@Component
public class PluginRegistry {

    public static final String CLASSPATH_URL = "classpath:";
    private static final String BOOTSTRAP_FILE = "/plugins.json";

    public static class PluginMetadata {
        @JsonProperty("name")
        public String name;
        @JsonProperty("class")
        public String className;
        @JsonProperty("url")
        public String url;

        @Override
        public String toString() {
            return String.format("%s: %s -- URL: %s", name, className, url);
        }
    }

    public static class PluginBundle {
        PluginMetadata metadata;
        AlertPlugin plugin;

        PluginBundle(PluginMetadata metadata, AlertPlugin plugin) {
            this.metadata = metadata;
            this.plugin = plugin;
        }

        public PluginMetadata getMetadata() {
            return metadata;
        }

        public AlertPlugin getPlugin() {
            return plugin;
        }
    }

    private static final Logger logger = Logger.getLogger(PluginRegistry.class);

    Set<PluginBundle> bundles = Sets.newHashSet();

    @Autowired
    JarClassLoader jcl;

    @Autowired
    public PluginRegistry(AlertManager alertManager, Context pluginsContext) {
        // TODO: for now only classpath loading of bootstrap file is supported
        List<PluginMetadata> pluginsMetadata = parse(BOOTSTRAP_FILE);
        logger.info("Found " + pluginsMetadata.size() + " bootstrap plugins");
        for (PluginMetadata md : pluginsMetadata) {
            logger.info(md);
        }
        loadAndInitializePlugins(pluginsMetadata, pluginsContext);
        activateAndRegisterPlugins(alertManager);
    }

    private void activateAndRegisterPlugins(AlertManager alertManager) {
        for (PluginBundle bundle : bundles) {
            Pager pager = bundle.getPlugin().activate();
            alertManager.addPager(pager);
        }
    }

    private void loadAndInitializePlugins(List<PluginMetadata> pluginsMetadata, Context ctx) {
        // TODO: currently only loads from the classpath - will JCL to load from other URLs
        for (PluginMetadata pmd : pluginsMetadata) {
            try {
                AlertPlugin plugin = (AlertPlugin) Class.forName(pmd.className).newInstance();
                plugin.startup(ctx);
                PluginBundle bundle = new PluginBundle(pmd, plugin);
                logger.info(String.format("Instantiated plugin %s: %s", plugin.getName(),
                        plugin.getDescription()));
                bundles.add(bundle);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                logger.error(String.format("Could not instantiate %s - %s [%s]", pmd,
                        pmd.className, e.getLocalizedMessage()));
            }
        }
    }

    private List<PluginMetadata> parse(String bootstrapPath) {
        ObjectMapper mapper = new ObjectMapper();
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class,
                PluginMetadata.class);
        try {
            return mapper.readValue(getClass().getResourceAsStream(bootstrapPath), type);
        } catch (IOException e) {
            logger.error("Could not locate bootstrap file: " + e.getLocalizedMessage());
            return Collections.emptyList();
        }
    }

    public PluginBundle getBundle(String fqn) {
        for (PluginBundle pb : bundles) {
            if (pb.getMetadata().className.equals(fqn)) {
                return pb;
            }
        }
        return null;
    }
}
