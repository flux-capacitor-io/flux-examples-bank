export interface Alert {
  level: AlertLevel;
  content: string;
  msShowTime?: number;
}

export type AlertLevel = "success" | "info" | "warning" | "danger"
