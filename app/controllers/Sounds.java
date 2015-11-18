package controllers;

import controllers.securesocial.SecureSocial;

import models.Sound;

import play.mvc.With;

/**
 * Este controlador es el encargador de manejar las canciones de los usuarios.
 *
 * @author Javier Maldonado
 */
@CRUD.For(Sound.class)
@With(SecureSocial.class)
public class Sounds {

}
