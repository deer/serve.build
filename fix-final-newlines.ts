#!/usr/bin/env -S deno run --allow-read --allow-write
import { walk } from "jsr:@std/fs/walk";

const EXTS = [".java", ".xml", ".ts", ".md", ".properties", ".yaml", ".yml"];

const root = Deno.cwd() + "/";
const fixed: string[] = [];

for await (const entry of walk(root, { exts: EXTS, includeDirs: false, skip: [/target/, /\.git/] })) {
  const bytes = await Deno.readFile(entry.path);
  if (bytes.length > 0 && bytes[bytes.length - 1] !== 0x0a) {
    const fixed_bytes = new Uint8Array(bytes.length + 1);
    fixed_bytes.set(bytes);
    fixed_bytes[bytes.length] = 0x0a;
    await Deno.writeFile(entry.path, fixed_bytes);
    fixed.push(entry.path.replace(root, ""));
  }
}

if (fixed.length > 0) {
  console.log(`Fixed ${fixed.length} file(s):`);
  fixed.forEach((f) => console.log(`  ${f}`));
} else {
  console.log("All files OK.");
}
