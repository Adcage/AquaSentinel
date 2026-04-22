import AMapLoader from "@amap/amap-jsapi-loader";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type AMapNS = any;

let cachedAMap: AMapNS | null = null;
let loadPromise: Promise<AMapNS> | null = null;

export function useAMap() {
  const loadAMap = (): Promise<AMapNS> => {
    if (cachedAMap) return Promise.resolve(cachedAMap);
    if (loadPromise) return loadPromise;

    const key = import.meta.env.VITE_AMAP_KEY as string | undefined;

    loadPromise = AMapLoader.load({
      key: key ?? "",
      version: "2.0",
      plugins: [
        "AMap.Polyline",
        "AMap.Polygon",
        "AMap.InfoWindow",
        "AMap.MouseTool",
      ],
    }).then((AMapInstance: AMapNS) => {
      cachedAMap = AMapInstance;
      return AMapInstance;
    });

    return loadPromise;
  };

  const hasKey = (): boolean => {
    const key = import.meta.env.VITE_AMAP_KEY as string | undefined;
    return !!key && key !== "your_amap_key_here";
  };

  return { loadAMap, hasKey };
}
