import { defineConfig } from "deepsec/config";

export default defineConfig({
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
