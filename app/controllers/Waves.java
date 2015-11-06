package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

import controllers.securesocial.*;

@With(SecureSocial.class)
public class Waves extends Controller {

    public static void home() {
        render();
    }

}