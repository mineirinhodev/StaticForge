// Response types matching the Spring Boot backend DTOs

export interface StaticSummaryResponse {
  id: string;
  name: string;
  contentScope: string[];
  language: string;
  status: string;
  maxMembers: number;
  currentMembers: number;
  dataCenter: string;
  server: string;
  openVacancies: number;
  createdAt: string;
}

export interface CharacterSummaryResponse {
  id: string;
  name: string;
  server: string;
  dataCenter: string;
  isVerified: boolean;
  avatarUrl: string;
  mainJobName: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
