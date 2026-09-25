import { Injectable, computed, signal } from '@angular/core';

export type Locale = 'fr' | 'en';

/** Dictionnaires plat : clé → texte. Le français est la langue de référence. */
const DICTIONNAIRES: Record<Locale, Record<string, string>> = {
  fr: {
    'app.titre': 'KFOKAM48 — Présence & Relecture',
    'nav.etudiant': 'Étudiant',
    'nav.relecteur': 'Relecteur',
    'nav.formateur': 'Formateur',
    'nav.langue': 'Changer de langue',

    'etudiant.kicker': 'Espace étudiant',
    'etudiant.titre': 'Marquez votre présence',
    'etudiant.tagline':
      'Saisissez le code à 6 caractères projeté par le formateur pour enregistrer votre présence à la session.',
    'etudiant.etiquette-code': 'Code de présence',
    'etudiant.placeholder': 'Ex. JQP4AP',
    'etudiant.action': 'Valider ma présence',
    'etudiant.succes': 'Présence enregistrée. Bonne session !',
    'erreur.CODE_INCONNU': 'Ce code ne correspond à aucune session. Vérifiez la saisie.',
    'erreur.CODE_EXPIRE': 'Ce code a expiré : la fenêtre de 15 minutes est passée.',
    'erreur.DEJA_PRESENT': 'Vous êtes déjà marqué présent à cette session.',
    'erreur.SESSION_CLOTUREE': 'La session est clôturée : il n est plus possible de pointer.',
    'erreur.ETUDIANT_INCONNU': 'Étudiant inconnu.',
    'erreur.RESEAU_INJOIGNABLE': 'Impossible de joindre le serveur. Vérifiez votre connexion.',
    'erreur.defaut': 'Une erreur inattendue est survenue.',
    'etudiant.en-attente': "Dépôt d'exercice et relectures arrivent avec les prochains tickets.",

    'relecteur.kicker': 'Espace relecteur',
    'relecteur.titre': 'Vos relectures assignées',
    'relecteur.tagline':
      "Les exercices qui vous ont été confiés pour relecture, note et commentaire à l'appui.",
    'relecteur.en-attente': 'Disponible avec les tickets EF9 à EF12.',

    'formateur.kicker': 'Espace formateur',
    'formateur.titre': 'Ouvrez une session',
    'formateur.tagline':
      'Créez la session, partagez le code à 6 caractères, puis suivez les présences et les relectures en direct.',
    'formateur.en-attente': 'Création de session (EF1) et tableau de suivi (EF16) arrivent ici.',
    'formateur.etiquette-titre': 'Titre de la session',
    'formateur.placeholder-titre': 'Ex. Algorithmique — séance 1',
    'formateur.action': 'Ouvrir la session',
    'formateur.session-ouverte': 'Session ouverte',
    'formateur.code-a-partager': 'Projetez ce code aux étudiants — ils le saisissent pour pointer.',
    'formateur.expire-dans': 'Le code expire dans',
    'formateur.minutes': 'min',
  },
  en: {
    'app.titre': 'KFOKAM48 — Attendance & Peer Review',
    'nav.etudiant': 'Student',
    'nav.relecteur': 'Reviewer',
    'nav.formateur': 'Instructor',
    'nav.langue': 'Switch language',

    'etudiant.kicker': 'Student area',
    'etudiant.titre': 'Check in to your session',
    'etudiant.tagline':
      'Enter the 6-character code shown by your instructor to record your attendance.',
    'etudiant.etiquette-code': 'Attendance code',
    'etudiant.placeholder': 'e.g. JQP4AP',
    'etudiant.action': 'Check in',
    'etudiant.succes': 'Attendance recorded. Enjoy your session!',
    'erreur.CODE_INCONNU': 'This code matches no session. Please double-check.',
    'erreur.CODE_EXPIRE': 'This code has expired: the 15-minute window has passed.',
    'erreur.DEJA_PRESENT': 'You are already checked in to this session.',
    'erreur.SESSION_CLOTUREE': 'The session is closed: check-in is no longer possible.',
    'erreur.ETUDIANT_INCONNU': 'Unknown student.',
    'erreur.RESEAU_INJOIGNABLE': 'Cannot reach the server. Check your connection.',
    'erreur.defaut': 'An unexpected error occurred.',
    'etudiant.en-attente': 'Exercise submission and peer reviews arrive with upcoming tickets.',

    'relecteur.kicker': 'Reviewer area',
    'relecteur.titre': 'Your assigned reviews',
    'relecteur.tagline':
      'Exercises assigned to you for review, with grade and comment.',
    'relecteur.en-attente': 'Available with tickets EF9 to EF12.',

    'formateur.kicker': 'Instructor area',
    'formateur.titre': 'Open a session',
    'formateur.tagline':
      'Create the session, share the 6-character code, then track attendance and reviews live.',
    'formateur.en-attente': 'Session creation (EF1) and tracking dashboard (EF16) land here.',
    'formateur.etiquette-titre': 'Session title',
    'formateur.placeholder-titre': 'e.g. Algorithms — session 1',
    'formateur.action': 'Open the session',
    'formateur.session-ouverte': 'Session is open',
    'formateur.code-a-partager': 'Project this code — students type it to check in.',
    'formateur.expire-dans': 'Code expires in',
    'formateur.minutes': 'min',
  },
};

/**
 * i18n minimaliste à base de signals : le changement de langue est réactif
 * dans toute l'application, sans dépendance externe. FR par défaut.
 */
@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly localeCourante = signal<Locale>('fr');

  readonly locale = this.localeCourante.asReadonly();

  readonly estFrancais = computed(() => this.localeCourante() === 'fr');

  traduire(cle: string): string {
    const dictionnaire = DICTIONNAIRES[this.localeCourante()];
    return dictionnaire[cle] ?? DICTIONNAIRES['fr'][cle] ?? cle;
  }

  basculer(): void {
    this.localeCourante.update((locale) => (locale === 'fr' ? 'en' : 'fr'));
  }
}
