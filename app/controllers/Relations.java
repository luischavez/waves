package controllers;

import controllers.securesocial.SecureSocial;

import models.Relation;

import play.mvc.With;

/**
 * Este controlador es el encargador de manejar las relaciones de los usuarios.
 *
 * @author Javier Maldonado
 */
@CRUD.For(Relation.class)
@With(SecureSocial.class)
public class Relations {

}
