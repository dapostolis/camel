/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.impl.cloud;

import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.model.cloud.ServiceCallConfigurationDefinition;
import org.apache.camel.model.cloud.ServiceCallExpressionConfiguration;
import org.apache.camel.model.language.SimpleExpression;
import org.junit.Assert;
import org.junit.Test;

public class ServiceCallConfigurationTest {

    // ****************************************
    // test default resolution
    // ****************************************

    @Test
    public void testDefaultConfigurationFromCamelContext() throws Exception {
        StaticServiceDiscovery sd = new StaticServiceDiscovery();
        sd.addServer("127.0.0.1:8080");
        sd.addServer("127.0.0.1:8081");

        BlacklistServiceFilter sf = new BlacklistServiceFilter();
        sf.addServer("127.0.0.1:8080");

        ServiceCallConfigurationDefinition conf = new ServiceCallConfigurationDefinition();
        conf.setServiceDiscovery(sd);
        conf.setServiceFilter(sf);

        CamelContext context = new DefaultCamelContext();
        context.setServiceCallConfiguration(conf);
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .routeId("default")
                    .serviceCall()
                        .name("scall")
                        .component("file")
                        .end();
            }
        });

        context.start();

        DefaultServiceCallProcessor proc = findServiceCallProcessor(context.getRoute("default"));

        Assert.assertNotNull(proc);
        Assert.assertTrue(proc.getLoadBalancer() instanceof DefaultLoadBalancer);

        DefaultLoadBalancer loadBalancer = (DefaultLoadBalancer)proc.getLoadBalancer();
        Assert.assertEquals(sd, loadBalancer.getServiceDiscovery());
        Assert.assertEquals(sf, loadBalancer.getServiceFilter());

        context.stop();
    }

    @Test
    public void testDefaultConfigurationFromRegistryWithDefaultName() throws Exception {
        StaticServiceDiscovery sd = new StaticServiceDiscovery();
        sd.addServer("127.0.0.1:8080");
        sd.addServer("127.0.0.1:8081");

        BlacklistServiceFilter sf = new BlacklistServiceFilter();
        sf.addServer("127.0.0.1:8080");

        ServiceCallConfigurationDefinition conf = new ServiceCallConfigurationDefinition();
        conf.setServiceDiscovery(sd);
        conf.serviceFilter(sf);

        SimpleRegistry reg = new SimpleRegistry();
        reg.put(org.apache.camel.model.cloud.ServiceCallConstants.DEFAULT_SERVICE_CALL_CONFIG_ID, conf);

        CamelContext context = new DefaultCamelContext(reg);
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .routeId("default")
                    .serviceCall()
                        .name("scall")
                        .component("file")
                        .end();
            }
        });

        context.start();

        DefaultServiceCallProcessor proc = findServiceCallProcessor(context.getRoute("default"));

        Assert.assertNotNull(proc);
        Assert.assertTrue(proc.getLoadBalancer() instanceof DefaultLoadBalancer);

        DefaultLoadBalancer loadBalancer = (DefaultLoadBalancer)proc.getLoadBalancer();
        Assert.assertEquals(sd, loadBalancer.getServiceDiscovery());
        Assert.assertEquals(sf, loadBalancer.getServiceFilter());

        context.stop();
    }


    @Test
    public void testDefaultConfigurationFromRegistryWithNonDefaultName() throws Exception {
        StaticServiceDiscovery sd = new StaticServiceDiscovery();
        sd.addServer("127.0.0.1:8080");
        sd.addServer("127.0.0.1:8081");

        BlacklistServiceFilter sf = new BlacklistServiceFilter();
        sf.addServer("127.0.0.1:8080");

        ServiceCallConfigurationDefinition conf = new ServiceCallConfigurationDefinition();
        conf.setServiceDiscovery(sd);
        conf.serviceFilter(sf);

        SimpleRegistry reg = new SimpleRegistry();
        reg.put(UUID.randomUUID().toString(), conf);

        CamelContext context = new DefaultCamelContext(reg);
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .routeId("default")
                    .serviceCall()
                        .name("scall")
                        .component("file")
                        .end();
            }
        });

        context.start();

        DefaultServiceCallProcessor proc = findServiceCallProcessor(context.getRoute("default"));

        Assert.assertNotNull(proc);
        Assert.assertTrue(proc.getLoadBalancer() instanceof DefaultLoadBalancer);

        DefaultLoadBalancer loadBalancer = (DefaultLoadBalancer)proc.getLoadBalancer();
        Assert.assertEquals(sd, loadBalancer.getServiceDiscovery());
        Assert.assertEquals(sf, loadBalancer.getServiceFilter());

        context.stop();
    }

    // ****************************************
    // test mixed resolution
    // ****************************************

    @Test
    public void testMixedConfiguration() throws Exception {
        // Default
        StaticServiceDiscovery defaultServiceDiscovery = new StaticServiceDiscovery();
        defaultServiceDiscovery.addServer("127.0.0.1:8080");
        defaultServiceDiscovery.addServer("127.0.0.1:8081");
        defaultServiceDiscovery.addServer("127.0.0.1:8082");

        BlacklistServiceFilter defaultServiceFilter = new BlacklistServiceFilter();
        defaultServiceFilter.addServer("127.0.0.1:8080");

        ServiceCallConfigurationDefinition defaultConfiguration = new ServiceCallConfigurationDefinition();
        defaultConfiguration.setServiceDiscovery(defaultServiceDiscovery);
        defaultConfiguration.serviceFilter(defaultServiceFilter);

        // Named
        BlacklistServiceFilter namedServiceFilter = new BlacklistServiceFilter();
        namedServiceFilter.addServer("127.0.0.1:8081");

        ServiceCallConfigurationDefinition namedConfiguration = new ServiceCallConfigurationDefinition();
        namedConfiguration.serviceFilter(namedServiceFilter);

        // Local
        StaticServiceDiscovery localServiceDiscovery = new StaticServiceDiscovery();
        localServiceDiscovery.addServer("127.0.0.1:8080");
        localServiceDiscovery.addServer("127.0.0.1:8081");
        localServiceDiscovery.addServer("127.0.0.1:8082");
        localServiceDiscovery.addServer("127.0.0.1:8084");


        // Camel context
        CamelContext context = new DefaultCamelContext();
        context.setServiceCallConfiguration(defaultConfiguration);
        context.addServiceCallConfiguration("named", namedConfiguration);
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:default")
                    .id("default")
                    .serviceCall()
                        .name("default-scall")
                        .component("file")
                        .end();
                from("direct:named")
                    .id("named")
                    .serviceCall()
                        .serviceCallConfiguration("named")
                        .name("named-scall")
                        .component("file")
                        .end();
                from("direct:local")
                    .id("local")
                    .serviceCall()
                        .serviceCallConfiguration("named")
                        .name("local-scall")
                        .component("file")
                        .serviceDiscovery(localServiceDiscovery)
                        .end();
            }
        });

        context.start();

        {
            // Default
            DefaultServiceCallProcessor proc = findServiceCallProcessor(context.getRoute("default"));

            Assert.assertNotNull(proc);
            Assert.assertTrue(proc.getLoadBalancer() instanceof DefaultLoadBalancer);

            DefaultLoadBalancer loadBalancer = (DefaultLoadBalancer) proc.getLoadBalancer();
            Assert.assertEquals(defaultServiceDiscovery, loadBalancer.getServiceDiscovery());
            Assert.assertEquals(defaultServiceFilter, loadBalancer.getServiceFilter());
        }

        {
            // Named
            DefaultServiceCallProcessor proc = findServiceCallProcessor(context.getRoute("named"));

            Assert.assertNotNull(proc);
            Assert.assertTrue(proc.getLoadBalancer() instanceof DefaultLoadBalancer);

            DefaultLoadBalancer loadBalancer = (DefaultLoadBalancer) proc.getLoadBalancer();
            Assert.assertEquals(defaultServiceDiscovery, loadBalancer.getServiceDiscovery());
            Assert.assertEquals(namedServiceFilter, loadBalancer.getServiceFilter());
        }

        {
            // Local
            DefaultServiceCallProcessor proc = findServiceCallProcessor(context.getRoute("local"));

            Assert.assertNotNull(proc);
            Assert.assertTrue(proc.getLoadBalancer() instanceof DefaultLoadBalancer);

            DefaultLoadBalancer loadBalancer = (DefaultLoadBalancer) proc.getLoadBalancer();
            Assert.assertEquals(localServiceDiscovery, loadBalancer.getServiceDiscovery());
            Assert.assertEquals(namedServiceFilter, loadBalancer.getServiceFilter());
        }

        context.stop();
    }

    // **********************************************
    // test placeholders
    // **********************************************

    @Test
    public void testPlaceholders() throws Exception {
        CamelContext context = null;

        try {
            System.setProperty("scall.name", "service-name");
            System.setProperty("scall.scheme", "file");

            context = new DefaultCamelContext();
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                        .routeId("default")
                        .serviceCall()
                            .name("{{scall.name}}")
                            .component("{{scall.scheme}}")
                            .uri("direct:{{scall.name}}")
                            .serviceDiscovery(new StaticServiceDiscovery())
                        .end();
                }
            });

            context.start();

            DefaultServiceCallProcessor proc = findServiceCallProcessor(context.getRoute("default"));

            Assert.assertNotNull(proc);
            Assert.assertTrue(proc.getLoadBalancer() instanceof DefaultLoadBalancer);
            Assert.assertEquals("service-name", proc.getName());
            Assert.assertEquals("file", proc.getScheme());
            Assert.assertEquals("direct:service-name", proc.getUri());

        } finally {
            if (context != null) {
                context.stop();
            }

            // Cleanup system properties
            System.clearProperty("scall.name");
            System.clearProperty("scall.component");
        }

        context.stop();
    }

    // **********************************************
    // test placeholders
    // **********************************************

    @Test
    public void testExpression() throws Exception {
        CamelContext context = null;

        try {
            ServiceCallConfigurationDefinition config = new ServiceCallConfigurationDefinition();
            config.setServiceDiscovery(new StaticServiceDiscovery());
            config.setExpressionConfiguration(
                new ServiceCallExpressionConfiguration().expression(
                    new SimpleExpression("file:${header.CamelServiceCallServiceHost}:${header.CamelServiceCallServicePort}")
                )
            );

            context = new DefaultCamelContext();
            context.setServiceCallConfiguration(config);
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                        .routeId("default")
                        .serviceCall("scall");
                }
            });

            context.start();

            DefaultServiceCallProcessor proc = findServiceCallProcessor(context.getRoute("default"));

            Assert.assertNotNull(proc);
            Assert.assertTrue(proc.getExpression() instanceof SimpleExpression);

        } finally {
            if (context != null) {
                context.stop();
            }
        }

        context.stop();
    }

    // **********************************************
    // Helper
    // **********************************************

    private DefaultServiceCallProcessor findServiceCallProcessor(Route route) {
        for (Processor processor : route.navigate().next()) {
            if (processor instanceof DefaultServiceCallProcessor) {
                return (DefaultServiceCallProcessor)processor;
            }
        }

        throw new IllegalStateException("Unable to find a ServiceCallProcessor");
    }

}
