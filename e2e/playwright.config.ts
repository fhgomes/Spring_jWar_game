import { defineConfig, devices } from "@playwright/test";

/**
 * Playwright configuration for the jWar end-to-end suite.
 *
 * The tests assume the docker-compose stack is already running on
 * `BASE_URL` (default http://localhost:8080). Bring it up with
 * `make up` from the repo root before invoking `npm test`.
 *
 * If we later want Playwright to manage the lifecycle itself, uncomment
 * the `webServer` block at the bottom of this file.
 */

const isCI = !!process.env.CI;

export default defineConfig({
  testDir: "./tests",
  timeout: 30_000,
  expect: {
    timeout: 5_000,
  },

  fullyParallel: true,
  forbidOnly: isCI,
  retries: isCI ? 1 : 0,
  workers: isCI ? 2 : undefined,

  reporter: [
    ["list"],
    ["html", { open: "never", outputFolder: "playwright-report" }],
  ],

  outputDir: "test-results",

  use: {
    baseURL: process.env.BASE_URL ?? "http://localhost:8080",
    trace: "on-first-retry",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
    actionTimeout: 10_000,
    navigationTimeout: 15_000,
  },

  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
    { name: "firefox", use: { ...devices["Desktop Firefox"] } },
    { name: "webkit", use: { ...devices["Desktop Safari"] } },
  ],

  // To let Playwright manage the stack itself, uncomment the block below
  // and remove the assumption that `make up` was run beforehand.
  //
  // webServer: {
  //   command: "docker compose up -d --wait",
  //   url: "http://localhost:8080/api/health",
  //   timeout: 120_000,
  //   reuseExistingServer: !isCI,
  // },
});
