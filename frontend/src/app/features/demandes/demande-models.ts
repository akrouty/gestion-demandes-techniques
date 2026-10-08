import { Role } from '../../core/session/session-models';
export const categories = {
  ETUDE_DANGERS: 'Étude de dangers',
  ANALYSE_RISQUES_INDUSTRIELS: 'Analyse des risques industriels',
  PROTECTION_INCENDIE: 'Protection incendie',
  NOTE_CALCUL: 'Note de calcul',
  DOSSIER_TECHNIQUE: 'Dossier technique',
  ASSISTANCE_REGLEMENTAIRE: 'Assistance réglementaire',
  AUTRE: 'Autre',
} as const;
export const priorites = {
  BASSE: 'Basse',
  MOYENNE: 'Moyenne',
  HAUTE: 'Haute',
  CRITIQUE: 'Critique',
} as const;
export const statuts = {
  NOUVELLE: 'Nouvelle',
  ASSIGNEE: 'Assignée',
  EN_COURS: 'En cours',
  RESOLUE: 'Résolue',
  CLOTUREE: 'Clôturée',
  ANNULEE: 'Annulée',
} as const;
export type Categorie = keyof typeof categories;
export type Priorite = keyof typeof priorites;
export type Statut = keyof typeof statuts;
export interface NouveauClientRequest {
  nom: string;
  email: string;
  telephone: string;
}
export interface ClientSummaryResponse extends NouveauClientRequest {
  id: number;
}
export interface AgentAssignableResponse {
  id: number;
  nom: string;
  email: string;
  actif: boolean;
}
export interface UtilisateurResumeResponse extends AgentAssignableResponse {
  roles: Role[];
}
export interface DemandeSummaryResponse {
  reference: string;
  titre: string;
  categorie: Categorie;
  priorite: Priorite;
  statut: Statut;
  client: ClientSummaryResponse;
  agentAffecte: UtilisateurResumeResponse | null;
  dateCreation: string;
  dateModification: string;
}
export interface DemandeDetailResponse extends DemandeSummaryResponse {
  description: string;
  createur: UtilisateurResumeResponse;
  descriptionTraitement: string | null;
  solution: string | null;
  motifAnnulation: string | null;
  dateResolution: string | null;
  dateCloture: string | null;
  dateAnnulation: string | null;
}
export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export type CreationDemandeRequest = {
  titre: string;
  description: string;
  categorie: Categorie;
  priorite: Priorite;
} & (
  | { clientId: number; nouveauClient?: never }
  | { nouveauClient: NouveauClientRequest; clientId?: never }
);
export interface QualificationDemandeRequest {
  categorie: Categorie;
  priorite: Priorite;
}
export interface TraitementDemandeRequest {
  descriptionTraitement?: string;
  solution?: string;
}
export interface DemandeQuery {
  page: number;
  size: number;
  sort: string;
  statut?: Statut;
  priorite?: Priorite;
  categorie?: Categorie;
  clientId?: number;
  agentId?: number;
  recherche?: string;
}
export interface ApiError {
  code: string;
  message: string;
  fieldErrors?: { field: string; code: string; message: string }[];
}
