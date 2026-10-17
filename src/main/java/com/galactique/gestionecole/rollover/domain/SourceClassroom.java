package com.galactique.gestionecole.rollover.domain;

import com.galactique.gestionecole.schoolclasses.domain.Classroom;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClass;

public record SourceClassroom(Classroom classroom, SchoolClass schoolClass) { }
