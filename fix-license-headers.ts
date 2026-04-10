#!/usr/bin/env -S deno run --allow-read --allow-write
import { walk } from "jsr:@std/fs/walk";

const LICENSE_PATTERN = /\/\*-.*?#L%\s*\*\//s;

async function fixFile(path: string): Promise<boolean> {
  const text = await Deno.readTextFile(path);

  const licenseMatch = text.match(LICENSE_PATTERN);
  if (!licenseMatch) return false;

  const licenseBlock = licenseMatch[0];
  const licenseStart = text.indexOf(licenseBlock);

  const packageMatch = text.match(/^package\s+/m);
  if (!packageMatch || packageMatch.index === undefined) return false;

  if (licenseStart <= packageMatch.index) return false; // already correct

  const withoutLicense = (
    text.slice(0, licenseStart).trimEnd() +
    text.slice(licenseStart + licenseBlock.length)
  ).trimStart();

  await Deno.writeTextFile(path, licenseBlock + "\n" + withoutLicense);
  return true;
}

const root = Deno.cwd();
const fixed: string[] = [];

for await (const entry of walk(root, { exts: [".java"], skip: [/target/], includeDirs: false })) {
  if (await fixFile(entry.path)) {
    fixed.push(entry.path.replace(root, ""));
  }
}

if (fixed.length > 0) {
  console.log(`Fixed ${fixed.length} file(s):`);
  fixed.forEach((f) => console.log(`  ${f}`));
} else {
  console.log("All files OK.");
}
