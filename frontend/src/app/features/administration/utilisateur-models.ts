import { Role } from '../../core/session/session-models';
export type RoleMetier = Exclude<Role, 'ADMINISTRATEUR'>;
export const roleLabels: Record<Role, string> = {
  RESPONSABLE_TECHNIQUE: 'Responsable technique',
  AGENT_TECHNIQUE: 'Agent technique',
  ADMINISTRATEUR: 'Administrateur',
};
export interface UtilisateurSummaryResponse {
  id: number;
  nom: string;
  email: string;
  actif: boolean;
  roles: Role[];
}
export interface UtilisateurDetailResponse {
  id: number;
  nom: string;
  email: string;
  actif: boolean;
  roles: Role[];
}
export interface CreationUtilisateurRequest {
  nom: string;
  email: string;
  actif: boolean;
  rolesMetier: RoleMetier[];
  password: string;
}
export interface ModificationUtilisateurRequest {
  nom: string;
  email: string;
}
export interface RolesMetierRequest {
  rolesMetier: RoleMetier[];
}
export interface PageUtilisateursResponse {
  items: UtilisateurSummaryResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export interface UtilisateurQuery {
  page: number;
  size: number;
  sort?: string;
}
export interface AdministrationError {
  code: string;
  message: string;
  fieldErrors?: { field: string; code: string; message: string }[];
}
