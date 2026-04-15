import type { DenoteConfig } from "@denote/core";

export const config: DenoteConfig = {
  name: "serve.build",
  logo: {
    text: "serve",
    suffix: ".build",
  },
  colors: {
    primary: "#e07b39",
    background: "#0c0c0b",
    surface: "#141412",
    text: "#edeae4",
    border: "#232320",
  },
  style: {
    roundedness: "none",
    darkMode: "dark",
    customCss: "static/custom.css",
  },
  landing: {
    hero: {
      badge: "Java 25",
      title: "HTTP without the",
      titleHighlight: "framework tax",
      description:
        "A lightweight server built directly on jdk.httpserver, virtual threads, and structured concurrency. Routes are code. No annotation scanning.",
    },
    cta: {
      primary: { text: "Get Started", href: "/docs/installation" },
      secondary: {
        text: "GitHub",
        href: "https://github.com/deer/serve.build",
      },
    },
    install: "build.serve:serve-foundation:0.1.0",
    features: [
      {
        icon: "⚡",
        title: "Virtual threads",
        description:
          "One virtual thread per request. No thread pools to tune, no reactive model to learn.",
      },
      {
        icon: "📦",
        title: "JPMS-native",
        description:
          "Every module declares explicit requires and exports. You know exactly what depends on what.",
      },
      {
        icon: "🔧",
        title: "Routes are code",
        description:
          "Wire routes with RouterBuilder. No annotation scanning, no classpath magic — just a configure() method.",
      },
      {
        icon: "🔌",
        title: "Pick what you need",
        description:
          "19 modules across core, middleware, protocol, and template layers. Add only what your application uses.",
      },
    ],
  },
  navigation: [
    {
      title: "Getting Started",
      children: [
        { title: "Introduction", href: "/docs/introduction" },
        { title: "Quickstart", href: "/docs/quickstart" },
      ],
    },
    {
      title: "Core",
      children: [
        { title: "Routing", href: "/docs/routing" },
        { title: "Middleware", href: "/docs/middleware" },
      ],
    },
    {
      title: "Protocols",
      children: [
        { title: "Protocols", href: "/docs/protocols" },
      ],
    },
  ],
  topNav: [
    { title: "Documentation", href: "/docs" },
    { title: "GitHub", href: "https://github.com/deer/serve.build" },
  ],
  social: {
    github: "https://github.com/deer/serve.build",
  },
  footer: {
    copyright: "© 2026 Reed von Redwitz",
  },
  search: {
    enabled: true,
  },
  ai: {
    mcp: true,
  },
};
