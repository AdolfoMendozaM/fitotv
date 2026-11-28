' Buscar canales, películas y series en la lista activa.
sub init()
    m.top.observeField("active", "onActiveChange")
    m.c = palette()
    uiBox(m.top, 0, 0, 1920, 1080, m.c.noche, 0)
    m.body = m.top.createChild("Group")
    m.rail = m.top.createChild("Rail")
    m.rail.current = "buscar"
    m.inRail = false
    m.query = ""
    m.zone = "field"
    c = m.c
    uiLabel(m.body, "Buscar", 168, 40, 60, c.text, "display")
    f = m.body.createChild("Group")
    f.translation = [168, 138]
    m.fieldRing = uiRing(f, 0, 0, 960, 84, 21)
    m.fieldBg = uiBox(f, 0, 0, 960, 84, c.s2, 21)
    uiIcon(f, "search", 27, 24, 36, c.muted)
    m.fieldText = uiLabel(f, "", 84, 22, 30, c.text, "semibold", 850)
    m.msg = uiLabel(m.body, "", 168, 258, 27, c.muted, "regular", 1600)
    m.countLb = uiLabel(m.body, "", 168, 250, 36, c.text, "display")
    m.grid = m.body.createChild("MarkupGrid")
    m.grid.itemComponentName = "ChannelCard"
    m.grid.numColumns = 5
    m.grid.numRows = 3
    m.grid.itemSize = [330, 198]
    m.grid.itemSpacing = [12, 12]
    m.grid.drawFocusFeedback = false
    m.grid.vertFocusAnimationStyle = "floatingFocus"
    m.grid.translation = [162, 318]
    m.grid.observeField("itemSelected", "onSelect")
    m.results = []
end sub

sub onActiveChange()
    if m.top.active then
        st = app("getState")
        if st.profile <> invalid then m.rail.avatar = st.profile.avatar
        if m.query <> "" then runSearch()
        render()
        focus()
    end if
end sub

sub runSearch()
    m.results = app("search", {q: m.query})
    cn = CreateObject("roSGNode", "ContentNode")
    m.liveZap = CreateObject("roSGNode", "ContentNode")
    for each r in m.results
        n = app("channelNode", {id: r.id})
        if n <> invalid then
            cn.appendChild(n.clone(false))
            if n.kind = "LIVE" then m.liveZap.appendChild(n.clone(false))
        end if
    end for
    m.grid.content = cn
end sub

sub render()
    c = m.c
    if m.query = "" then
        m.fieldText.text = "Canal, película o serie"
        m.fieldText.color = "0x5C6FB0FF"
    else
        m.fieldText.text = m.query
        m.fieldText.color = c.text
    end if
    m.countLb.text = ""
    m.msg.text = ""
    m.grid.visible = false
    if Len(m.query.Trim()) < 2 then
        m.msg.text = "Escribe al menos dos letras. Presiona OK para abrir el teclado."
    else if m.results.Count() = 0 then
        m.msg.text = "No encontramos " + Chr(34) + m.query.Trim() + Chr(34) + " en esta lista."
    else
        m.countLb.text = "Resultados  " + m.results.Count().ToStr()
        m.grid.visible = true
    end if
    paint()
end sub

sub paint()
    f = not m.inRail and m.zone = "field"
    m.fieldRing.visible = f
    if f then m.fieldBg.blendColor = m.c.s3 else m.fieldBg.blendColor = m.c.s2
end sub

sub focus()
    if not m.inRail and m.zone = "grid" and m.grid.visible then
        m.grid.setFocus(true)
    else
        m.top.setFocus(true)
    end if
    paint()
end sub

sub onSelect()
    n = m.grid.content.getChild(m.grid.itemSelected)
    if n = invalid then return
    sfx("ok")
    if n.kind = "SERIES" and n.url = "" and n.seriesId <> "" then
        app("go", {screen: "SeriesScreen", params: {id: n.id}})
    else if n.kind = "LIVE" then
        app("play", {id: n.id, zap: m.liveZap})
    else
        app("play", {id: n.id})
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.inRail then return railKey(key)
    if m.zone = "field" then
        if key = "OK" then
            sfx("ok")
            uiKeyboard("Buscar", m.query, "search")
            return true
        else if key = "down" and m.grid.visible then
            m.zone = "grid"
            sfx("tick")
            focus()
            return true
        else if key = "left" then
            railEnter()
            paint()
            return true
        end if
        return key <> "back"
    end if
    ' llegan aquí las teclas que el grid no usó
    if key = "up" or key = "back" then
        m.zone = "field"
        sfx("tick")
        focus()
        return true
    else if key = "left" then
        railEnter()
        paint()
        m.top.setFocus(true)
        return true
    end if
    return false
end function

sub railLeft()
    focus()
end sub

function onKeyboardText(p as object) as dynamic
    m.query = p.text.Trim()
    runSearch()
    render()
    if m.grid.visible then m.zone = "grid" else m.zone = "field"
    focus()
    return true
end function
