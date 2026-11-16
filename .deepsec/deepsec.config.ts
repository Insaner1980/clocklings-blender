import { defineConfig, regexMatcher } from "deepsec/config";

const networkConnectionSlug = "clocklings-kotlin-network-connection";

export default defineConfig({
  plugins: [
    {
      name: "clocklings-android",
      matchers: [
        {
          slug: networkConnectionSlug,
          description: "Review Android network destinations, consent, and transmitted application data.",
          noiseTier: "normal",
          filePatterns: ["app/src/main/**/*.kt"],
          match: (content) => regexMatcher(
            networkConnectionSlug,
            [{ regex: /\.\s*openConnection\s*\(/, label: "Opens a network connection" }],
            content,
          ),
        },
      ],
    },
  ],
  projects: [
    {
      id: "clocklings",
      root: "..",
      promptAppend:
        "Prioritize exported Android components, local progress and settings integrity, backup exclusions, offline text-to-speech voice selection, and sensitive logging.",
      priorityPaths: [
        "app/src/main/AndroidManifest.xml",
        "app/src/main/res/xml/",
        "app/src/main/java/fi/clocklings/data/",
        "app/src/main/java/fi/clocklings/audio/"
      ]
    }
  ]
});
