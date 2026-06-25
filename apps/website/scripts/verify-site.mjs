import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { dirname, join, relative } from "node:path";
import { fileURLToPath } from "node:url";

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const dist = join(root, "dist");

const requiredRoutes = [
  "index.html",
  "features/index.html",
  "privacy/index.html",
  "terms/index.html",
  "safety/index.html",
  "ai-transparency/index.html",
  "support/index.html",
  "data-deletion/index.html",
  "accessibility/index.html",
  "legal/index.html",
  "brand/index.html",
  "faq/index.html",
  "404.html"
];

const redirectsFile = join(root, "public", "_redirects");

const routeFromHref = (href) => {
  const clean = href.split("#")[0].split("?")[0];
  if (!clean || clean === "/") {
    return "index.html";
  }
  return `${clean.replace(/^\/+|\/+$/g, "")}/index.html`;
};

const listFiles = (folder) => {
  const entries = readdirSync(folder);
  return entries.flatMap((entry) => {
    const path = join(folder, entry);
    return statSync(path).isDirectory() ? listFiles(path) : [path];
  });
};

const fail = (message) => {
  console.error(message);
  process.exitCode = 1;
};

if (!existsSync(dist)) {
  fail("Missing dist directory. Run npm run build first.");
}

for (const route of requiredRoutes) {
  const path = join(dist, route);
  if (!existsSync(path)) {
    fail(`Missing required route: ${route}`);
  }
}

if (!existsSync(join(dist, "sitemap-index.xml"))) {
  fail("Missing sitemap-index.xml.");
}

if (!existsSync(join(dist, "robots.txt"))) {
  fail("Missing robots.txt.");
}

if (existsSync(redirectsFile)) {
  const redirects = readFileSync(redirectsFile, "utf8").split(/\r?\n/);
  redirects.forEach((line, index) => {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      return;
    }
    const [source] = trimmed.split(/\s+/);
    if (!source.startsWith("/")) {
      fail(`Invalid _redirects source on line ${index + 1}: ${source}. Use a relative path source.`);
    }
  });
}

const htmlFiles = listFiles(dist).filter((file) => file.endsWith(".html"));
const allHtml = htmlFiles.map((file) => readFileSync(file, "utf8")).join("\n");

const blockedPatterns = [
  [/localhost/i, "Hardcoded localhost URL found."],
  [/lorem ipsum/i, "Placeholder lorem ipsum found."],
  [/account dashboard/i, "False account dashboard wording found."],
  [/log in to delete/i, "False login-to-delete wording found."],
  [/bettamind requires an account/i, "False account requirement found."],
  [/ai therapist/i, "False AI therapist claim found."],
  [/cures anxiety/i, "Medical cure claim found."],
  [/treats depression/i, "Medical treatment claim found."],
  [/guaranteed transformation/i, "Unsupported guaranteed outcome claim found."],
  [/fully anonymous/i, "Unsupported anonymity claim found."]
];

for (const [pattern, message] of blockedPatterns) {
  if (pattern.test(allHtml)) {
    fail(message);
  }
}

for (const file of htmlFiles) {
  const html = readFileSync(file, "utf8");
  const links = [...html.matchAll(/href="([^"]+)"/g)].map((match) => match[1]);
  for (const href of links) {
    if (
      href.startsWith("http") ||
      href.startsWith("mailto:") ||
      href.startsWith("tel:") ||
      href.startsWith("#") ||
      href.startsWith("/assets/") ||
      href.startsWith("/_astro/")
    ) {
      continue;
    }
    if (!href.startsWith("/")) {
      continue;
    }
    const target = join(dist, routeFromHref(href));
    if (!existsSync(target)) {
      fail(`Broken internal link in ${relative(dist, file)}: ${href}`);
    }
  }
}

if (!process.exitCode) {
  console.log(`Verified ${htmlFiles.length} HTML files, required routes, sitemap and internal links.`);
}
