import sitemap from "@astrojs/sitemap";
import { defineConfig } from "astro/config";

export default defineConfig({
  site: "https://www.bettamind.com",
  output: "static",
  trailingSlash: "never",
  integrations: [sitemap()]
});
