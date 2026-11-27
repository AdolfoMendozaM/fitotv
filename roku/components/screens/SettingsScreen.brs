' Ajustes: sonidos, listas, actualizar, perfil y acerca de.
sub init()
    m.top.observeField("active", "onActiveChange")
    m.c = palette()
    uiBox(m.top, 0, 0, 1920, 1080, m.c.noche, 0)
    m.body = m.top.createChild("Group")
    m.rail = m.top.createChild("Rail")
    m.rail.current = "ajustes"
    m.inRail = false
    m.idx = 0
    m.about = false
end sub

sub onActiveChange()
    if m.top.active then
        render()
        m.top.setFocus(true)
    end if
end sub

sub render()
    c = m.c
    uiClear(m.body)
    st = app("getState")
    if st.profile <> invalid then m.rail.avatar = st.profile.avatar
    pname = "este perfil"
    if st.profile <> invalid then pname = st.profile.name
    uiLabel(m.body, "Ajustes", 168, 60, 69, c.text, "display")
    m.items = []
    m.items.Push({key: "sound", title: "Sonidos", sub: "Los sonidos de marimba al moverte y al abrir FitoTV"})
    m.items.Push({key: "lists", title: "Mis listas", sub: st.lists.Count().ToStr() + " de " + pname})
    if st.activeList <> invalid then m.items.Push({key: "refresh", title: "Actualizar " + st.activeList.name + " ahora", sub: "Normalmente se actualiza sola cada vez que abres FitoTV"})
    who = ""
    if st.profile <> invalid then who = st.profile.name
    m.items.Push({key: "profile", title: "Cambiar de perfil", sub: "Ahora: " + who})
    ver = CreateObject("roAppInfo").GetVersion()
    m.items.Push({key: "about", title: "Acerca de FitoTV", sub: "Versión " + ver + ". Las actualizaciones llegan solas por Roku."})
    if m.idx >= m.items.Count() then m.idx = m.items.Count() - 1
    y = 186
    m.rows = []
    sound = st.settings.sound <> false
    for each it in m.items
        g = m.body.createChild("Group")
        g.translation = [168, y]
        ring = uiRing(g, 0, 0, 1230, 120, 24)
        bg = uiBox(g, 0, 0, 1230, 120, c.s1, 24)
        uiLabel(g, it.title, 33, 22, 29, c.text, "semibold", 1000)
        uiLabel(g, it.sub, 33, 66, 23, c.muted, "regular", 1000)
        if it.key = "sound" then
            col = "0x2A3F86FF"
            dx = 6
            dc = c.muted2
            if sound then
                col = c.menta
                dx = 48
                dc = c.white
            end if
            uiPill(g, 1230 - 33 - 90, 39, 90, 42, col)
            uiCircle(g, 1230 - 33 - 90 + dx, 45, 30, dc)
        else if it.key = "lists" then
            uiLabel(g, "Abrir", 1230 - 33 - 200, 44, 24, c.muted, "regular", 200, 1, "right")
        else if it.key = "refresh" then
            uiIcon(g, "refresh", 1230 - 33 - 36, 42, 36, c.muted2)
        else if it.key = "profile" then
            uiIcon(g, "user", 1230 - 33 - 36, 42, 36, c.muted2)
        else if it.key = "about" then
            uiIcon(g, "info", 1230 - 33 - 36, 42, 36, c.muted2)
        end if
        m.rows.Push({ring: ring, bg: bg})
        y = y + 141
    end for
    if m.about then uiLabel(m.body, "FitoTV es solo un reproductor: no incluye canales. Reproduce las listas que tú agregas, de proveedores que tengas permiso de usar.", 168, y + 6, 24, c.muted, "regular", 1230, 3)
    paint()
end sub

sub paint()
    for i = 0 to m.rows.Count() - 1
        f = not m.inRail and i = m.idx
        m.rows[i].ring.visible = f
        if f then m.rows[i].bg.blendColor = m.c.s3 else m.rows[i].bg.blendColor = m.c.s1
    end for
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.inRail then return railKey(key)
    if key = "up" then
        if m.idx > 0 then
            m.idx = m.idx - 1
            sfx("tick")
            paint()
        end if
        return true
    else if key = "down" then
        if m.idx < m.items.Count() - 1 then
            m.idx = m.idx + 1
            sfx("tick")
            paint()
        end if
        return true
    else if key = "left" then
        railEnter()
        paint()
        return true
    else if key = "OK" then
        k = m.items[m.idx].key
        if k = "sound" then
            st = app("getState")
            on = not (st.settings.sound <> false)
            app("setSound", {on: on})
            if on then sfx("on")
            render()
        else if k = "lists" then
            sfx("ok")
            app("go", {screen: "ListsScreen"})
        else if k = "refresh" then
            sfx("ok")
            app("refreshActive", {quiet: true})
            app("toast", {title: "Actualizando la lista", text: "Tarda unos segundos. Puedes seguir viendo."})
        else if k = "profile" then
            sfx("ok")
            app("goTab", {name: "perfil"})
        else if k = "about" then
            sfx("ok")
            m.about = not m.about
            render()
        end if
        return true
    end if
    return false
end function

sub railLeft()
    paint()
    m.top.setFocus(true)
end sub

function onKeyboardText(p as object) as dynamic
    return true
end function
