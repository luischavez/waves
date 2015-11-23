#{form @Public.lang(), class:'form-inline'}
    <div class="form-group">
        <select name="locale" id="locale" class="form-control input-lg" onchange="this.form.submit()">
            #{if "es".equals(play.i18n.Lang.get())}
                <option value="es" selected>es</option>
            #{/if}
            #{else}
                <option value="es">es</option>
            #{/else}

            #{if "en".equals(play.i18n.Lang.get())}
                <option value="en" selected>en</option>
            #{/if}
            #{else}
                <option value="en">en</option>
            #{/else}
        </select>
    </div>
#{/form}