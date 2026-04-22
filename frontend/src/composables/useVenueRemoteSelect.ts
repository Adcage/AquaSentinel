import { ref, shallowRef, watch } from "vue";
import { getVenueVoById, listVenueVoByPage } from "@/api/venueController";
import { unwrapApiData } from "@/services/serviceUtils";
import { useVenueStore } from "@/stores/venueStore";

type VenueOptionValue = string | number;

export interface VenueOption<T extends VenueOptionValue> {
  label: string;
  value: T;
}

interface UseVenueRemoteSelectConfig {
  valueType: "string" | "number";
  status?: number;
  pageSize?: number;
  errorMessage?: string;
}

export function useVenueRemoteSelect<T extends VenueOptionValue>(
  config: UseVenueRemoteSelectConfig,
) {
  const venueStore = useVenueStore();
  const venueOptions = shallowRef<Array<VenueOption<T>>>([]);
  const venueLoading = ref(false);
  const hasMore = ref(true);

  let keyword = "";
  let current = 1;
  const pageSize = config.pageSize ?? 50;
  let initialized = false;

  const parseValue = (id: number): T => {
    if (config.valueType === "number") {
      return Number(id) as T;
    }
    return String(id) as T;
  };

  const mergeOptions = (incoming: Array<VenueOption<T>>) => {
    const merged = [...venueOptions.value];
    incoming.forEach((option) => {
      if (!merged.some((item) => item.value === option.value)) {
        merged.push(option);
      }
    });
    venueOptions.value = merged;
  };

  const mapRecords = (
    records: API.VenueVO[] | undefined,
  ): Array<VenueOption<T>> => {
    return (records ?? [])
      .filter((item: API.VenueVO) => item.id != null)
      .map((item: API.VenueVO) => ({
        label: item.venueName || `${item.id}号场馆`,
        value: parseValue(Number(item.id)),
      }));
  };

  const resetPagination = () => {
    current = 1;
    hasMore.value = true;
    venueOptions.value = [];
  };

  const refreshOptions = async () => {
    keyword = "";
    resetPagination();
    await loadNextPage();
  };

  const loadNextPage = async () => {
    if (venueLoading.value || !hasMore.value) {
      return;
    }
    venueLoading.value = true;
    try {
      const response = await listVenueVoByPage({
        current,
        pageSize,
        venueName: keyword || undefined,
        status: config.status,
      });
      const pageData = unwrapApiData<API.PageVenueVO>(
        response,
        config.errorMessage || "加载场馆列表失败",
      );
      const records = pageData?.records ?? [];
      const mappedOptions = mapRecords(records);
      if (current === 1) {
        venueOptions.value = mappedOptions;
      } else {
        mergeOptions(mappedOptions);
      }

      const total = Number(pageData?.total ?? 0);
      const loadedCount = venueOptions.value.length;
      hasMore.value =
        records.length >= pageSize && (total <= 0 || loadedCount < total);
      if (hasMore.value) {
        current += 1;
      }
      initialized = true;
    } finally {
      venueLoading.value = false;
    }
  };

  const handleVenueRemoteSearch = async (query: string) => {
    keyword = query.trim();
    resetPagination();
    await loadNextPage();
  };

  const handleVenueVisibleChange = async (visible: boolean) => {
    if (!visible) {
      return;
    }
    if (initialized && venueOptions.value.length) {
      return;
    }
    await refreshOptions();
  };

  const handleVenuePopupScroll = async (event: Event) => {
    const target = event.target as HTMLElement | null;
    if (!target) {
      return;
    }
    const nearBottom =
      target.scrollTop + target.clientHeight >= target.scrollHeight - 12;
    if (nearBottom) {
      await loadNextPage();
    }
  };

  const ensureVenueOption = async (
    value: string | number | null | undefined,
  ) => {
    if (value == null || value === "") {
      return;
    }
    const venueId = Number(value);
    if (!Number.isFinite(venueId) || venueId <= 0) {
      return;
    }
    if (
      venueOptions.value.some(
        (option) => Number(option.value) === Number(venueId),
      )
    ) {
      return;
    }
    const response = await getVenueVoById({ id: venueId });
    const venue = unwrapApiData<API.VenueVO>(response, "加载场馆详情失败");
    if (venue?.id == null) {
      return;
    }
    mergeOptions([
      {
        label: venue.venueName || `${venue.id}号场馆`,
        value: parseValue(Number(venue.id)),
      },
    ]);
  };

  watch(
    () => venueStore.revision,
    () => {
      if (!initialized) {
        return;
      }
      void refreshOptions();
    },
  );

  return {
    venueOptions,
    venueLoading,
    hasMore,
    loadNextPage,
    handleVenueRemoteSearch,
    handleVenueVisibleChange,
    handleVenuePopupScroll,
    ensureVenueOption,
    refreshOptions,
  };
}
