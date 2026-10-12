import { Router } from "express";
import {
  createLotController,
  createMedicationController,
  adjustLotStockController,
  getExpiringLotsController,
  getLotsController,
  getLowStockLotsController,
  getMedicationsController,
} from "../controllers/inventory.controller";

const router = Router();

router.post("/medications", createMedicationController);
router.get("/medications", getMedicationsController);
router.post("/lots", createLotController);
router.get("/lots", getLotsController);
router.patch("/lots/:id/stock", adjustLotStockController);
router.get("/alerts/low-stock", getLowStockLotsController);
router.get("/alerts/expiring", getExpiringLotsController);

export default router;
