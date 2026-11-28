' "¿Quién va a ver?": elige perfil, agrega uno o adminístralos.
sub init()
    m.top.observeField("active", "onActiveChange")
    m.c = palette()
    uiBox(m.top, 0, 0, 1920, 1080, m.c.noche, 0)
    m.g = m.top.createChild("Group")
    m.editing = false
    m.row = 0
    m.col = 0
end sub

sub onActiveChange()
    if m.top.active then
        render()
        m.top.setFocus(true)
    end if
end sub

sub render()
    uiClear(m.g)
    c = m.c
    st = app("getState")
    m.profiles = st.profiles
    title = "¿Quién va a ver?"
    if m.editing then title = "Administrar perfiles"
    uiLabel(m.g, title, 0, 200, 78, c.text, "display", 1920, 1, "center")
    n = m.profiles.Count() + 1
    size = 225
    gap = 60
    total = n * size + (n - 1) * gap
    x0 = (1920 - total) / 2
    m.tiles = []
    for i = 0 to n - 1
        t = m.g.createChild("Group")
        t.translation = [x0 + i * (size + gap), 380]
        ring = t.createChild("Poster")
        ring.uri = "pkg:/images/ring128.png"
        ring.width = size + 24
        ring.height = size + 24
        ring.translation = [-12, -12]
        ring.blendColor = "0xFFFFFFFF"
        ring.visible = false
        if i < m.profiles.Count() then
            p = m.profiles[i]
            uiAvatar(t, p.avatar, 0, 0, size)
            if m.editing then
                uiCircle(t, 0, 0, size, "0x0A1747A0")
                uiIcon(t, "edit", size / 2 - 30, size / 2 - 30, 60, "0xFFFFFFFF")
            end if
            nm = uiLabel(t, p.name, -40, size + 30, 33, c.muted2, "semibold", size + 80, 1, "center")
            if p.kids = true then uiLabel(t, "Solo infantil", -40, size + 76, 22, c.menta, "medium", size + 80, 1, "center")
        else
            uiCircle(t, 0, 0, size, "0x12235FFF")
            uiIcon(t, "plus", size / 2 - 40, size / 2 - 40, 80, c.num)
            nm = uiLabel(t, "Agregar perfil", -40, size + 30, 33, c.muted2, "semibold", size + 80, 1, "center")
        end if
        m.tiles.Push({node: t, ring: ring, name: nm})
    end for
    btxt = "Administrar perfiles"
    if m.editing then btxt = "Listo"
    m.btn = uiButton(m.g, 0, 820, btxt, "edit", "secondary")
    m.btn.node.translation = [(1920 - m.btn.w) / 2, 820]
    if m.col >= n then m.col = n - 1
    paint()
end sub

sub paint()
    for i = 0 to m.tiles.Count() - 1
        f = m.row = 0 and i = m.col
        m.tiles[i].ring.visible = f
        if f then m.tiles[i].name.color = "0xFFFFFFFF" else m.tiles[i].name.color = "0xC8D2F5FF"
    end for
    uiButtonFocus(m.btn, m.row = 1)
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if key = "left" and m.row = 0 and m.col > 0 then
        m.col = m.col - 1
    else if key = "right" and m.row = 0 and m.col < m.tiles.Count() - 1 then
        m.col = m.col + 1
    else if key = "down" and m.row = 0 then
        m.row = 1
    else if key = "up" and m.row = 1 then
        m.row = 0
    else if key = "OK" then
        if m.row = 1 then
            sfx("ok")
            m.editing = not m.editing
            render()
            return true
        end if
        if m.col >= m.profiles.Count() then
            sfx("ok")
            app("go", {screen: "CreateProfileScreen", params: {firstTime: false}})
        else if m.editing then
            sfx("ok")
            app("go", {screen: "CreateProfileScreen", params: {firstTime: false, editId: m.profiles[m.col].id}})
        else
            sfx("success")
            app("startSession", {id: m.profiles[m.col].id})
            app("reset", {screen: "HolaScreen"})
        end if
        return true
    else if key = "back" and m.editing then
        m.editing = false
        render()
        return true
    else
        return key <> "back"
    end if
    sfx("tick")
    paint()
    return true
end function

function onKeyboardText(p as object) as dynamic
    return true
end function
