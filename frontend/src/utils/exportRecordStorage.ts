export interface ExportRecord {
  name: string;
  type: string;
  operator: string;
  createdAt: string;
}

const STORAGE_KEY = "statistics_export_records";

const MAX_RECORDS = 50;

export const exportRecordStorage = {
  getRecords(): ExportRecord[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return [];
      }
      const records = JSON.parse(raw) as ExportRecord[];
      if (!Array.isArray(records)) {
        return [];
      }
      return records;
    } catch {
      return [];
    }
  },

  addRecord(record: ExportRecord): void {
    const records = this.getRecords();
    const newRecords = [record, ...records].slice(0, MAX_RECORDS);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(newRecords));
    } catch {
      // localStorage 可能已满，忽略错误
    }
  },

  clearRecords(): void {
    localStorage.removeItem(STORAGE_KEY);
  },
};
