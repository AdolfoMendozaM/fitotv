' Crea o edita un perfil: nombre, avatar (13) y perfil infantil.
sub init()
    m.top.observeField("active", "onActiveChange")
    m.c = palette()
    c = m.c
    uiBox(m.top, 0, 0, 1920, 1080, c.noche, 0)
    m.g = m.top.createChild("Group")
    m.zone = 0
    m.av = 0
    m.btn = 0
    m.confirmDelete = false
    m.name = ""
    m.avatar = "nina"
    m.kids = false
    m.editing = invalid
    m.ready = false
end sub

sub onActiveChange()
    if not m.top.active then return
    if not m.ready then
        m.ready = true
        p = m.top.params
        m.firstTime = p.firstTime = true
        if p.editId <> invalid then
            for each pr in app("getState").profiles
                if pr.id = p.editId then m.editing = pr
            end for
        end if
        if m.editing <> invalid then
            m.name = m.editing.name
            m.avatar = avatarKey(m.editing.avatar)
            m.kids = m.editing.kids = true
        end if
        keys = avatarKeys()
        for i = 0 to keys.Count() - 1
            if keys[i] = m.avatar then m.av = i
        end for
        render()
    end if
    m.top.setFocus(true)
end sub

sub render()
    uiClear(m.g)
    c = m.c
    title = "Crea tu perfil"
    if m.editing <> invalid then title = "Edita el perfil"
    uiLabel(m.g, title, 144, 90, 72, c.text, "display")
    uiLabel(m.g, "Así tus listas, favoritos y recientes quedan a tu nombre.", 144, 186, 30, c.muted)
    uiLabel(m.g, "Tu nombre", 144, 262, 27, c.text, "semibold")
    m.field = m.g.createChild("Group")
    m.field.translation = [144, 304]
    m.fieldBg = uiBox(m.field, 0, 0, 756, 90, "0x0F1F5CFF", 18)
    m.fieldRing = uiRing(m.field, 0, 0, 756, 90, 18)
    if m.name = "" then
        m.fieldText = uiLabel(m.field, "Escribe tu nombre", 30, 26, 30, c.dim, "regular", 700)
    else
        m.fieldText = uiLabel(m.field, m.name, 30, 26, 30, c.text, "medium", 700)
    end if
    uiLabel(m.g, "Elige tu avatar", 144, 438, 27, c.text, "semibold")
    keys = avatarKeys()
    m.avTiles = []
    for i = 0 to keys.Count() - 1
        col = i mod 7
        row = i \ 7
        t = m.g.createChild("Group")
        t.translation = [144 + col * 138, 486 + row * 138]
        sel = t.createChild("Poster")
        sel.uri = "pkg:/images/ring128.png"
        sel.blendColor = c.menta
        sel.width = 117
        sel.height = 117
        sel.visible = false
        ring = t.createChild("Poster")
        ring.uri = "pkg:/images/ring128.png"
        ring.blendColor = "0xFFFFFFFF"
        ring.width = 133
        ring.height = 133
        ring.translation = [-8, -8]
        ring.visible = false
        img = uiAvatar(t, keys[i], 0, 0, 117)
        m.avTiles.Push({node: t, sel: sel, ring: ring, img: img, key: keys[i]})
    end for
    m.kidsBox = m.g.createChild("Group")
    m.kidsBox.translation = [144, 790]
    m.kidsBg = uiBox(m.kidsBox, 0, 0, 840, 105, "0x0F1F5CFF", 27)
    m.kidsRing = uiRing(m.kidsBox, 0, 0, 840, 105, 27)
    uiLabel(m.kidsBox, "Perfil infantil", 30, 18, 29, c.text, "semibold")
    uiLabel(m.kidsBox, "Solo muestra canales del grupo Infantil", 30, 58, 23, c.muted)
    m.switchBg = uiPill(m.kidsBox, 720, 33, 84, 42, "0x2A3F86FF")
    m.switchDot = uiCircle(m.kidsBox, 726, 39, 30, "0xC8D2F5FF")
    m.btns = []
    x = 144
    if not m.firstTime then
        b = uiButton(m.g, x, 960, "Atrás", "", "secondary")
        b.action = "back"
        m.btns.Push(b)
        x = x + b.w + 24
    end if
    label = "Continuar"
    if m.editing <> invalid then label = "Guardar"
    b = uiButton(m.g, x, 960, label, "", "primary")
    b.action = "save"
    m.btns.Push(b)
    x = x + b.w + 24
    if m.editing <> invalid then
        t = "Borrar perfil"
        if m.confirmDelete then t = "Presiona otra vez para borrar"
        b = uiButton(m.g, x, 960, t, "trash", "secondary")
        b.action = "delete"
        m.btns.Push(b)
    end if
    for i = 0 to m.btns.Count() - 1
        if m.btns[i].action = "save" and m.btn = 0 then m.btn = i
    end for
    ' vista previa
    pv = m.g.createChild("Group")
    pv.translation = [1380, 300]
    ring = pv.createChild("Poster")
    ring.uri = "pkg:/images/ring128.png"
    ring.blendColor = c.menta
    ring.width = 375
    ring.height = 375
    m.preview = uiAvatar(pv, m.avatar, 21, 21, 333)
    nm = m.name
    col = c.text
    if nm = "" then
        nm = "Tu nombre"
        col = c.dim
    end if
    m.previewName = uiLabel(pv, nm, -100, 410, 51, col, "display", 575, 1, "center")
    paint()
end sub

sub paint()
    m.fieldRing.visible = m.zone = 0
    for i = 0 to m.avTiles.Count() - 1
        t = m.avTiles[i]
        t.sel.visible = t.key = m.avatar
        t.ring.visible = m.zone = 1 and i = m.av
    end for
    m.kidsRing.visible = m.zone = 2
    if m.kids then
        m.switchBg.blendColor = "0x3DE0BEFF"
        m.switchDot.translation = [768, 39]
        m.switchDot.blendColor = "0xFFFFFFFF"
    else
        m.switchBg.blendColor = "0x2A3F86FF"
        m.switchDot.translation = [726, 39]
        m.switchDot.blendColor = "0xC8D2F5FF"
    end if
    for i = 0 to m.btns.Count() - 1
        uiButtonFocus(m.btns[i], m.zone = 3 and i = m.btn)
    end for
end sub

function onKeyboardText(p as object) as dynamic
    m.name = Left(p.text.Trim(), 20)
    render()
    m.top.setFocus(true)
    return true
end function

sub doSave()
    if m.editing <> invalid then
        app("updateProfile", {id: m.editing.id, name: m.name, avatar: m.avatar, kids: m.kids})
        sfx("success")
        app("back")
    else if m.firstTime then
        id = app("createProfile", {name: m.name, avatar: m.avatar, kids: m.kids})
        app("startSession", {id: id})
        sfx("success")
        app("reset", {screen: "AddListScreen", params: {firstTime: true}})
    else
        app("createProfile", {name: m.name, avatar: m.avatar, kids: m.kids})
        sfx("success")
        app("back")
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if key = "up" then
        if m.zone = 1 and m.av >= 7 then
            m.av = m.av - 7
        else if m.zone > 0 then
            m.zone = m.zone - 1
        end if
    else if key = "down" then
        if m.zone = 1 and m.av + 7 < m.avTiles.Count() then
            m.av = m.av + 7
        else if m.zone = 1 and m.av < 7 then
            m.av = m.avTiles.Count() - 1
        else if m.zone < 3 then
            m.zone = m.zone + 1
        end if
    else if key = "left" then
        if m.zone = 1 and m.av mod 7 > 0 then m.av = m.av - 1
        if m.zone = 3 and m.btn > 0 then m.btn = m.btn - 1
    else if key = "right" then
        if m.zone = 1 and m.av mod 7 < 6 and m.av < m.avTiles.Count() - 1 then m.av = m.av + 1
        if m.zone = 3 and m.btn < m.btns.Count() - 1 then m.btn = m.btn + 1
    else if key = "OK" then
        if m.zone = 0 then
            sfx("ok")
            uiKeyboard("Tu nombre", m.name, "name")
        else if m.zone = 1 then
            sfx("tick")
            m.avatar = m.avTiles[m.av].key
            m.preview.uri = avatarUri(m.avatar)
        else if m.zone = 2 then
            m.kids = not m.kids
            if m.kids then sfx("on") else sfx("off")
        else
            a = m.btns[m.btn].action
            if a = "save" then
                doSave()
                return true
            else if a = "back" then
                sfx("back")
                app("back")
                return true
            else if a = "delete" then
                if m.confirmDelete then
                    app("deleteProfile", {id: m.editing.id})
                    st = app("getState")
                    if st.profiles.Count() = 0 then
                        app("reset", {screen: "TutorialScreen"})
                    else
                        app("reset", {screen: "ProfilesScreen"})
                    end if
                    return true
                end if
                m.confirmDelete = true
                render()
                return true
            end if
        end if
        paint()
        return true
    else
        return key <> "back"
    end if
    sfx("tick")
    paint()
    return true
end function
