import { defineStore } from "pinia";
import { ref } from "vue";

export const useVenueStore = defineStore("venue", () => {
  const revision = ref(0);

  const bumpRevision = () => {
    revision.value += 1;
  };

  return {
    revision,
    bumpRevision,
  };
});
