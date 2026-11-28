' Menú de la izquierda: perfil, Inicio, Guía, Películas, Series, Buscar y Ajustes.
sub init()
    m.items = [
        {key: "perfil", label: "", icon: ""},
        {key: "inicio", label: "Inicio", icon: "home"},
        {key: "guia", label: "Guía", icon: "guide"},
        {key: "peliculas", label: "Películas", icon: "film"},
        {key: "series", label: "Series", icon: "tv"},
        {key: "buscar", label: "Buscar", icon: "search"},
        {key: "ajustes", label: "Ajustes", icon: "settings"}
    ]
    m.g = m.top.createChild("Group")
    render()
end sub

sub render()
    uiClear(m.g)
    c = palette()
    uiBox(m.g, 0, 0, 132, 1080, "0x0D1C55FF", 0)
    uiBox(m.g, 131, 0, 2, 1080, "0x1A2C6EFF", 0)
    uiImage(m.g, "pkg:/images/logo_small.png", 36, 30, 60, 60)
    m.nodes = []
    for i = 0 to m.items.Count() - 1
        it = m.items[i]
        y = 0
        if i = 0 then
            y = 118
        else if it.key = "ajustes" then
            y = 940
        else
            y = 200 + (i - 1) * 118
        end if
        t = m.g.createChild("Group")
        t.translation = [18, y]
        bg = uiBox(t, 0, 0, 96, 96, "0x00000000", 22)
        ring = uiRing(t, 0, 0, 96, 96, 22)
        if it.key = "perfil" then
            uiAvatar(t, m.top.avatar, 12, 12, 72)
            ic = invalid
            lb = invalid
        else
            ic = uiIcon(t, it.icon, 30, 14, 36, c.muted)
            lb = uiLabel(t, it.label, -12, 58, 18, c.muted, "semibold", 120, 1, "center")
        end if
        m.nodes.Push({bg: bg, ring: ring, icon: ic, label: lb, key: it.key})
    end for
    paint()
end sub

sub paint()
    if m.nodes = invalid then return
    for i = 0 to m.nodes.Count() - 1
        n = m.nodes[i]
        selected = n.key = m.top.current
        focused = m.top.hasFocus and i = m.top.focusIndex
        n.ring.visible = focused
        col = "0x00000000"
        if selected then col = "0x2350E0FF"
        if focused and not selected then col = "0x1A2E78FF"
        if n.key <> "perfil" then n.bg.blendColor = col
        if n.icon <> invalid then
            tc = "0xAEBBEBFF"
            if selected or focused then tc = "0xFFFFFFFF"
            n.icon.blendColor = tc
            n.label.color = tc
        end if
    end for
end sub
