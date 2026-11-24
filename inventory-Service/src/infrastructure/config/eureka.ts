import { Eureka } from 'eureka-js-client';

export function registerWithEureka(port: number): Eureka {
  const host = process.env.EUREKA_HOST || 'localhost';
  const eurekaPort = Number(process.env.EUREKA_PORT) || 8761;

  const client = new Eureka({
    instance: {
      app: 'INVENTORY-SERVICE',
      instanceId: `inventory-service:${port}`,
      hostName: 'localhost',
      ipAddr: '127.0.0.1',
      statusPageUrl: `http://localhost:${port}/`,
      port: { $: port, '@enabled': true },
      vipAddress: 'inventory-service',
      dataCenterInfo: {
        '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
        name: 'MyOwn',
      },
    },
    eureka: { host, port: eurekaPort, servicePath: '/eureka/apps/' },
  });

  client.start((error: unknown) => {
    console.log(error ? `Error registrando en Eureka: ${error}` : 'Inventario registrado en Eureka');
  });

  const shutdown = () => client.stop(() => process.exit(0));
  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);

  return client;
}