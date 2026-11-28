' Bienvenida en tres pasos, igual que en Android.
sub init()
    m.top.observeField("active", "onActiveChange")
    m.step = 0
    m.focus = 1
    m.c = palette()
    uiBox(m.top, 0, 0, 1920, 1080, m.c.noche, 0)
    top = m.top.createChild("Group")
    uiImage(top, "pkg:/images/logo_small.png", 72, 44, 60, 60)
    uiLabel(top, "FitoTV", 146, 50, 36, m.c.text, "display")
    m.page = m.top.createChild("Group")
    m.dots = m.top.createChild("Group")
    m.btns = []
    render()
end sub

sub onActiveChange()
    if m.top.active then m.top.setFocus(true)
end sub

sub render()
    uiClear(m.page)
    c = m.c
    titles = ["Trae tus listas", "Un perfil para cada quien", "Cambia de canal como siempre"]
    texts = ["Agrega las listas que ya tienes: con un enlace, un archivo o el usuario y contraseña que te dio tu proveedor.", "Cada quien con su avatar, sus listas y sus favoritos. Los peques pueden tener un perfil solo con canales infantiles.", "Muévete por grupos con el control, ve una vista previa al pasar por un canal y revisa qué sigue en la guía."]
    uiLabel(m.page, "Paso " + (m.step + 1).ToStr() + " de 3", 144, 300, 24, c.menta, "bold")
    uiLabel(m.page, titles[m.step], 144, 344, 66, c.text, "display", 860, 2)
    uiLabel(m.page, texts[m.step], 144, 520, 30, c.muted2, "regular", 760, 4)
    art = m.page.createChild("Group")
    art.translation = [1080, 250]
    if m.step = 0 then
        cards = [["Casa de mamá", "812 canales", "0x3DE0BEFF"], ["Deportes", "120 canales", "0x2350E0FF"], ["Canales abiertos", "48 canales", "0xB79CFFFF"]]
        for i = 0 to 2
            g = art.createChild("Group")
            g.translation = [i * 40, i * 150]
            uiBox(g, 0, 0, 560, 120, "0x1A2E78FF", 24)
            uiBox(g, 28, 28, 64, 64, cards[i][2], 16)
            uiLabel(g, Left(cards[i][0], 1), 28, 40, 30, "0x0A1747FF", "bold", 64, 1, "center")
            uiLabel(g, cards[i][0], 118, 30, 30, c.text, "semibold")
            uiLabel(g, cards[i][1], 118, 72, 24, c.muted)
        end for
    else if m.step = 1 then
        people = [["papa", "Fito"], ["mama", "Mamá"], ["abuela", "Abuela"], ["nina", "Peques"]]
        for i = 0 to 3
            x = i * 175
            uiAvatar(art, people[i][0], x, 60, 150)
            uiLabel(art, people[i][1], x - 10, 228, 27, c.text, "semibold", 170, 1, "center")
            if i = 3 then uiLabel(art, "Solo infantil", x - 10, 268, 21, c.menta, "medium", 170, 1, "center")
        end for
    else
        uiBox(art, 0, 0, 640, 300, "0x1A2E78FF", 28)
        uiBox(art, 40, 60, 330, 180, "0x2350E0FF", 16)
        uiCircle(art, 440, 100, 100, "0x0A1747FF")
        uiPill(art, 30, 20, 150, 36, "0x0A1747CC")
        uiLabel(art, "Vista previa", 48, 26, 20, c.text, "semibold")
        for i = 0 to 2
            g = art.createChild("Group")
            g.translation = [i * 222, 340]
            col = "0x12235FFF"
            if i = 1 then col = "0x1A2E78FF"
            uiBox(g, 0, 0, 200, 110, col, 16)
            uiLabel(g, (11 + i).ToStr(), 18, 14, 26, c.num, "display")
            uiLabel(g, ["Canal Ciudad", "Noticias 24", "Mundo Hoy"][i], 18, 62, 22, c.text, "medium", 170)
            if i = 1 then
                r = uiRing(g, 0, 0, 200, 110, 16)
                r.visible = true
            end if
        end for
    end if
    ' botones
    for each b in m.btns
        m.top.removeChild(b.node)
    end for
    m.btns = []
    x = 144
    if m.step > 0 then
        b = uiButton(m.top, x, 900, "Atrás", "", "secondary")
        b.action = "prev"
        m.btns.Push(b)
        x = x + b.w + 24
    end if
    label = "Siguiente"
    if m.step = 2 then label = "Crear mi perfil"
    b = uiButton(m.top, x, 900, label, "", "primary")
    b.action = "next"
    m.btns.Push(b)
    s = uiButton(m.top, 1640, 44, "Saltar", "", "ghost", 0, 60)
    s.action = "skip"
    m.btns.Push(s)
    ' puntos
    uiClear(m.dots)
    dx = x + b.w + 48
    for i = 0 to 2
        w = 16
        col = "0x2A3F86FF"
        if i = m.step then
            w = 44
            col = "0x3DE0BEFF"
        end if
        uiPill(m.dots, dx, 930, w, 16, col)
        dx = dx + w + 12
    end for
    m.focus = 0
    for i = 0 to m.btns.Count() - 1
        if m.btns[i].action = "next" then m.focus = i
    end for
    paint()
end sub

sub paint()
    for i = 0 to m.btns.Count() - 1
        uiButtonFocus(m.btns[i], i = m.focus)
    end for
end sub

sub activate()
    a = m.btns[m.focus].action
    if a = "prev" then
        sfx("back")
        m.step = m.step - 1
        render()
    else if a = "next" and m.step < 2 then
        sfx("step")
        m.step = m.step + 1
        render()
    else
        sfx("ok")
        app("go", {screen: "CreateProfileScreen", params: {firstTime: true}})
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if key = "left" then
        if m.focus > 0 and m.btns[m.focus].action <> "skip" then
            m.focus = m.focus - 1
            sfx("tick")
            paint()
        end if
        return true
    else if key = "right" then
        if m.focus < m.btns.Count() - 2 then
            m.focus = m.focus + 1
            sfx("tick")
            paint()
        end if
        return true
    else if key = "up" then
        m.focus = m.btns.Count() - 1
        sfx("tick")
        paint()
        return true
    else if key = "down" then
        for i = 0 to m.btns.Count() - 1
            if m.btns[i].action = "next" then m.focus = i
        end for
        paint()
        return true
    else if key = "OK" then
        activate()
        return true
    else if key = "back" and m.step > 0 then
        sfx("back")
        m.step = m.step - 1
        render()
        return true
    end if
    return false
end function

function onKeyboardText(p as object) as dynamic
    return true
end function
