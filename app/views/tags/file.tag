${_sound.name}
<a href="@{controllers.Application.streamFile(_sound.id)}" class="btn btn-primary">
    <span class="glyphicon glyphicon-play" aria-hidden="true"></span>
    &{'waves.playSound'}
</a>
<a href="@{controllers.Application.download(_sound.id)}" class="btn btn-default">
    <span class="glyphicon glyphicon-download" aria-hidden="true"></span>
    &{'waves.download'}
</a>
#{if _user.email.equals(_sound.owner.email)}
    <a href="@{controllers.Application.deleteFile(_sound.id)}" class="btn btn-warning" data-toggle="confirmation" data-original-title="" title="&{'waves.confirm'}">
        <span class="glyphicon glyphicon-trash" aria-hidden="true"></span>
        &{'waves.deleteSound'}
    </a>
#{/if}