' Manejo del menú de la izquierda, compartido por las pantallas principales.
' Cada pantalla define m.rail (nodo Rail) y m.inRail (bool) y llama railKey(key).
function railKey(key as string) as boolean
    n = 7
    if key = "up" then
        if m.rail.focusIndex > 0 then m.rail.focusIndex = m.rail.focusIndex - 1
        sfx("tick")
        return true
    else if key = "down" then
        if m.rail.focusIndex < n - 1 then m.rail.focusIndex = m.rail.focusIndex + 1
        sfx("tick")
        return true
    else if key = "right" then
        m.inRail = false
        m.rail.hasFocus = false
        sfx("tick")
        railLeft()
        return true
    else if key = "OK" then
        keys = ["perfil", "inicio", "guia", "peliculas", "series", "buscar", "ajustes"]
        k = keys[m.rail.focusIndex]
        sfx("ok")
        if k = m.rail.current then
            m.inRail = false
            m.rail.hasFocus = false
            railLeft()
            return true
        end if
        app("goTab", {name: k})
        return true
    end if
    return false
end function

sub railEnter()
    m.inRail = true
    m.rail.hasFocus = true
    keys = ["perfil", "inicio", "guia", "peliculas", "series", "buscar", "ajustes"]
    for i = 0 to keys.Count() - 1
        if keys[i] = m.rail.current then m.rail.focusIndex = i
    end for
    sfx("tick")
end sub
