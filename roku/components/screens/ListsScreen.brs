' Las listas del perfil: elegir cuál ver, actualizar, quitar o agregar otra.
sub init()
    m.top.observeField("active", "onActiveChange")
    m.c = palette()
    uiBox(m.top, 0, 0, 1920, 1080, m.c.noche, 0)
    m.g = m.top.createChild("Group")
    m.row = -1
    m.col = 0
    m.confirm = ""
end sub

sub onActiveChange()
    if m.top.active then
        scene().observeFieldScoped("listStatus", "onStatus")
        render()
        m.top.setFocus(true)
    else
        scene().unobserveFieldScoped("listStatus")
    end if
end sub

sub onStatus()
    s = scene().listStatus
    if s = invalid then return
    if s.state = "ready" or s.state = "error" or s.state = "none" then render()
end sub

function subtitleOf(l as object) as string
    if l.lastError <> invalid and l.lastError <> "" then return "No se pudo actualizar: " + l.lastError
    kind = "Enlace"
    if l.kind = "FILE" then kind = "Archivo"
    if l.kind = "XTREAM" then kind = "Usuario y contraseña"
    upd = ""
    if l.updatedAt <> invalid and l.updatedAt > 0 then upd = ", actualizada " + dayMonthTime(l.updatedAt)
    n = 0
    if l.channelCount <> invalid then n = l.channelCount
    return n.ToStr() + " canales, " + kind + upd
end function

sub render()
    c = m.c
    uiClear(m.g)
    st = app("getState")
    m.lists = st.lists
    active = st.activeList
    m.activeId = ""
    if active <> invalid then m.activeId = active.id
    pname = ""
    if st.profile <> invalid then pname = st.profile.name
    uiLabel(m.g, "Listas de " + pname, 144, 84, 69, c.text, "display", 1600)
    uiLabel(m.g, "Elige cuál quieres ver. Cada perfil tiene las suyas.", 144, 176, 30, c.muted, "regular", 1600)
    if m.row < 0 then
        m.row = 0
        for i = 0 to m.lists.Count() - 1
            if m.lists[i].id = m.activeId then m.row = i
        end for
    end if
    if m.row > m.lists.Count() then m.row = m.lists.Count()
    st2 = scene().listStatus
    busyId = ""
    if st2 <> invalid and (st2.state = "loading" or st2.state = "refreshing") and st2.id <> invalid then busyId = st2.id
    m.rows = []
    m.listG = m.g.createChild("Group")
    y = 264
    for i = 0 to m.lists.Count() - 1
        l = m.lists[i]
        g = m.listG.createChild("Group")
        g.translation = [144, y]
        ring = uiRing(g, 0, 0, 1050, 132, 24)
        bg = uiBox(g, 0, 0, 1050, 132, c.s1, 24)
        uiBox(g, 30, 27, 78, 78, c.menta, 21)
        uiLabel(g, UCase(Left(l.name, 1)), 30, 44, 33, c.noche, "bold", 78, 1, "center")
        uiLabel(g, l.name, 132, 30, 30, c.text, "semibold", 700)
        subc = c.muted
        if l.lastError <> invalid and l.lastError <> "" then subc = c.ambar
        sub1 = subtitleOf(l)
        if l.id = busyId then
            sub1 = "Actualizando…"
            subc = c.menta
        end if
        uiLabel(g, sub1, 132, 76, 23, subc, "regular", 740)
        if l.id = m.activeId then
            uiPill(g, 1050 - 30 - 132, 45, 132, 42, "0x3DE0BE33")
            uiLabel(g, "Viendo", 1050 - 30 - 132, 52, 22, c.menta, "semibold", 132, 1, "center")
        end if
        acts = []
        ax = 1050 + 18
        a1 = smallAction(g, ax, 33, "refresh", "Actualizar")
        acts.Push(a1)
        ax = ax + a1.w + 15
        label = "Quitar"
        if m.confirm = l.id then label = "¿Seguro? Otra vez"
        a2 = smallAction(g, ax, 33, "trash", label)
        acts.Push(a2)
        m.rows.Push({ring: ring, bg: bg, acts: acts})
        y = y + 153
    end for
    ' agregar
    g = m.listG.createChild("Group")
    g.translation = [144, y]
    ring = uiRing(g, 0, 0, 1050, 132, 24)
    out = g.createChild("Poster")
    out.uri = radiusFile(24, true)
    out.blendColor = c.line3
    out.width = 1050
    out.height = 132
    bg = uiBox(g, 0, 0, 1050, 132, c.clear, 24)
    uiBox(g, 30, 27, 78, 78, c.s2, 21)
    uiIcon(g, "plus", 49, 46, 40, c.num)
    uiLabel(g, "Agregar una lista", 132, 30, 30, c.text, "semibold", 880)
    uiLabel(g, "Desde tu celular con el QR, sin escribir con el control", 132, 76, 23, c.muted, "regular", 880)
    m.rows.Push({ring: ring, bg: bg, acts: [], add: true})
    uiLabel(m.g, "Presiona Atrás para cerrar", 144, 1010, 23, c.muted)
    paint()
end sub

function smallAction(parent as object, x as float, y as float, icon as string, label as string) as object
    c = m.c
    g = parent.createChild("Group")
    g.translation = [x, y]
    probe = uiLabel(g, label, 0, 0, 24, c.text, "semibold")
    w = probe.boundingRect().width + 27 + 30 + 12 + 27
    g.removeChild(probe)
    ring = uiRing(g, 0, 0, w, 66, 0, true)
    bg = uiPill(g, 0, 0, w, 66, c.clear)
    ic = uiIcon(g, icon, 27, 18, 30, c.muted2)
    lb = uiLabel(g, label, 27 + 30 + 12, 18, 24, c.muted2, "semibold")
    return {ring: ring, bg: bg, icon: ic, label: lb, w: w}
end function

sub paint()
    c = m.c
    shift = m.row - 3
    if shift < 0 then shift = 0
    m.listG.translation = [0, -shift * 153]
    for i = 0 to m.rows.Count() - 1
        r = m.rows[i]
        f = i = m.row and m.col = 0
        r.ring.visible = f
        base = c.s1
        if r.add = true then base = c.clear
        fc = c.s3
        if r.add = true then fc = c.s2
        if f then r.bg.blendColor = fc else r.bg.blendColor = base
        for j = 0 to r.acts.Count() - 1
            a = r.acts[j]
            af = i = m.row and m.col = j + 1
            a.ring.visible = af
            if af then a.bg.blendColor = c.s2 else a.bg.blendColor = c.clear
            if af then a.label.color = c.white else a.label.color = c.muted2
            a.icon.blendColor = a.label.color
        end for
    end for
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    n = m.rows.Count()
    if key = "up" then
        if m.row > 0 then
            m.row = m.row - 1
            m.col = 0
            sfx("tick")
            paint()
        end if
        return true
    else if key = "down" then
        if m.row < n - 1 then
            m.row = m.row + 1
            m.col = 0
            sfx("tick")
            paint()
        end if
        return true
    else if key = "left" then
        if m.col > 0 then
            m.col = m.col - 1
            sfx("tick")
            paint()
        end if
        return true
    else if key = "right" then
        if m.col < m.rows[m.row].acts.Count() then
            m.col = m.col + 1
            sfx("tick")
            paint()
        end if
        return true
    else if key = "OK" then
        if m.rows[m.row].add = true then
            sfx("ok")
            app("go", {screen: "AddListScreen", params: {firstTime: false}})
            return true
        end if
        l = m.lists[m.row]
        if m.col = 0 then
            sfx("ok")
            if l.id <> m.activeId then app("setActiveList", {id: l.id})
            app("back")
        else if m.col = 1 then
            sfx("ok")
            m.confirm = ""
            if l.id <> m.activeId then
                app("setActiveList", {id: l.id})
            else
                app("refreshActive", {quiet: true})
            end if
            render()
        else if m.col = 2 then
            if m.confirm = l.id then
                sfx("off")
                m.confirm = ""
                app("removeList", {id: l.id})
                if m.row > 0 then m.row = m.row - 1
                m.col = 0
            else
                sfx("tick")
                m.confirm = l.id
            end if
            render()
        end if
        return true
    end if
    return false
end function

function onKeyboardText(p as object) as dynamic
    return true
end function
