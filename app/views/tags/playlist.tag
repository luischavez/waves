${_playlist.name}
<a href="@{controllers.Application.playlist(_playlist.id)}" class="btn btn-primary">
    <span class="glyphicon glyphicon-eye-open" aria-hidden="true"></span>
    &{'waves.showPlaylist'}
</a>
#{if _user.email.equals(_playlist.owner.email)}
<a href="@{controllers.Application.deletePlaylist(_playlist.id)}" class="btn btn-warning" data-toggle="confirmation" data-original-title="" title="&{'waves.confirm'}">
    <span class="glyphicon glyphicon-trash" aria-hidden="true"></span>
    &{'waves.deletePlaylist'}
</a>
#{/if}