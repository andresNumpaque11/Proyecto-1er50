
import dotenv from 'dotenv';
import express, { Request, Response } from 'express';
import cors from 'cors';
import { connectDB } from './infrastructure/config/database';
import inventoryRoutes from './infrastructure/http/routes/inventory.routes';
import { registerWithEureka } from './infrastructure/config/eureka';
import { collectHttpMetrics, registry } from './infrastructure/monitoring/prometheus';

dotenv.config({ path: '.env' });

const app = express();
const PORT = Number(process.env.PORT) || 7879;

app.use(express.json());
app.use(cors());
app.use(collectHttpMetrics);

app.get('/metrics', async (_req: Request, res: Response) => {
  res.set('Content-Type', registry.contentType);
  res.end(await registry.metrics());
});

app.use('/api/inventory', inventoryRoutes);

app.get('/', (_req: Request, res: Response) => {
  res.status(200).json({
    message: 'El microservicio de Inventario está funcionando.',
    timestamp: new Date().toISOString(),
  });
});

async function startServer(): Promise<void> {
  try {
    await connectDB();
    app.listen(PORT, () => {
      console.log(`Microservicio de Inventario ejecutándose en el puerto ${PORT}`);
      registerWithEureka(PORT);
    });
  } catch (error) {
    console.error('Error al iniciar el microservicio:', error);
    process.exit(1);
  }
}
startServer();