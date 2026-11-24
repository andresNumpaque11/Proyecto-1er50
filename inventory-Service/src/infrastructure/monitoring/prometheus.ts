import type { NextFunction, Request, Response } from "express";
import {
  Counter,
  Histogram,
  Registry,
  collectDefaultMetrics,
} from "@prometheus-io/client";

export const registry = new Registry();

collectDefaultMetrics({ register: registry });

const requests = new Counter({
  name: "http_requests_total",
  help: "Total HTTP requests",
  labelNames: ["method", "route", "status_code"] as const,
  registers: [registry],
});

const requestDuration = new Histogram({
  name: "http_request_duration_seconds",
  help: "HTTP request duration in seconds",
  labelNames: ["method", "route", "status_code"] as const,
  buckets: [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5],
  registers: [registry],
});

export function collectHttpMetrics(
  req: Request,
  res: Response,
  next: NextFunction,
): void {
  if (req.path === "/metrics") {
    next();
    return;
  }

  const start = process.hrtime.bigint();
  res.once("finish", () => {
    const route = req.route
      ? `${req.baseUrl}${req.route.path}`
      : "unmatched";
    const labels = {
      method: req.method,
      route,
      status_code: String(res.statusCode),
    };
    requests.inc(labels);
    requestDuration.observe(
      labels,
      Number(process.hrtime.bigint() - start) / 1e9,
    );
  });
  next();
}
