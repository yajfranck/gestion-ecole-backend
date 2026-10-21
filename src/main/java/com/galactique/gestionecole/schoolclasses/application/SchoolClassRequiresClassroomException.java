package com.galactique.gestionecole.schoolclasses.application;

public class SchoolClassRequiresClassroomException extends RuntimeException {
	public SchoolClassRequiresClassroomException() {
		super("Cette opération est impossible : aucune salle de classe n'est associée à cette classe. Veuillez d'abord lui affecter au moins une salle.");
	}
}