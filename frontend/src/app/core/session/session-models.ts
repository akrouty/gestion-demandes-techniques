export type Role = 'RESPONSABLE_TECHNIQUE' | 'AGENT_TECHNIQUE' | 'ADMINISTRATEUR';
export interface UserSnapshot {
  id: number;
  nom: string;
  email: string;
  roles: readonly Role[];
}
export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: string;
  user: UserSnapshot;
}
export interface LoginRequest {
  email: string;
  password: string;
}
