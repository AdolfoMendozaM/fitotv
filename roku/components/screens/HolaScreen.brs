' "Hola, Fito": saluda mientras se abre la lista del perfil.
sub init()
    m.top.observeField("active", "onActiveChange")
    m.c = palette()
    uiBox(m.top, 0, 0, 1920, 1080, m.c.noche, 0)
    glow = uiCircle(m.top, 560, 40, 800, "0x13298A80")
    m.g = m.top.createChild("Group")
    m.minTimer = m.top.createChild("Timer")
    m.minTimer.duration = 1.6
    m.minTimer.observeField("fire", "onMin")
    m.ready = false
    m.minDone = false
    m.started = false
end sub

sub onActiveChange()
    if not m.top.active or m.started then return
    m.started = true
    m.top.setFocus(true)
    st = app("getState")
    c = m.c
    p = st.profile
    name = ""
    av = "nina"
    if p <> invalid then
        name = p.name
        av = p.avatar
    end if
    ring = m.g.createChild("Poster")
    ring.uri = "pkg:/images/ring128.png"
    ring.blendColor = c.menta
    ring.width = 270
    ring.height = 270
    ring.translation = [825, 250]
    uiAvatar(m.g, av, 840, 265, 240)
    uiLabel(m.g, "Hola, " + name, 0, 560, 84, c.text, "display", 1920, 1, "center")
    l = st.activeList
    m.sub = m.g.createChild("Group")
    if l <> invalid then
        txt = l.name
        if l.channelCount > 0 then txt = txt + "   " + l.channelCount.ToStr() + " canales"
        pw = 120 + Len(txt) * 15
        px = (1920 - pw) / 2
        uiPill(m.sub, px, 690, pw, 64, "0x12235FFF")
        uiBox(m.sub, px + 14, 702, 40, 40, c.menta, 12)
        uiLabel(m.sub, UCase(Left(l.name, 1)), px + 14, 708, 22, c.noche, "bold", 40, 1, "center")
        uiLabel(m.sub, txt, px + 70, 706, 27, c.text, "semibold", pw - 80)
    end if
    uiSpinner(m.g, 800, 790, 36, c.menta)
    uiLabel(m.g, "Abriendo tu inicio", 850, 790, 27, c.muted2, "medium")
    scene().observeFieldScoped("listStatus", "onStatus")
    onStatus()
    m.minTimer.control = "start"
end sub

sub onStatus()
    s = scene().listStatus
    if s = invalid then return
    if s.state = "ready" or s.state = "error" or s.state = "none" or s.state = "added" then
        m.ready = true
        maybeGo()
    end if
end sub

sub onMin()
    m.minDone = true
    maybeGo()
end sub

sub maybeGo()
    if not (m.ready and m.minDone) then return
    scene().unobserveFieldScoped("listStatus")
    s = scene().listStatus
    if s.state = "none" then
        app("reset", {screen: "AddListScreen", params: {firstTime: true}})
    else
        app("reset", {screen: "HomeScreen"})
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    return press and key <> "back"
end function

function onKeyboardText(p as object) as dynamic
    return true
end function
