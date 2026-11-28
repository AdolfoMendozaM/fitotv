' Pantalla de carga: el logo y "Preparando todo" / "Hola de nuevo".
sub init()
    m.top.observeField("active", "onActiveChange")
    c = palette()
    uiBox(m.top, 0, 0, 1920, 1080, c.noche, 0)
    m.logo = uiImage(m.top, "pkg:/images/logo.png", 852, 300, 216, 216)
    m.logo.scaleRotateCenter = [108, 108]
    m.word = uiLabel(m.top, "FitoTV", 0, 548, 96, c.text, "display", 1920, 1, "center")
    m.msg = uiLabel(m.top, "", 0, 690, 30, c.muted, "medium", 1920, 1, "center")
    m.anim = m.top.createChild("Animation")
    m.anim.duration = 0.7
    m.anim.easeFunction = "outCubic"
    sc = m.anim.createChild("Vector2DFieldInterpolator")
    sc.key = [0.0, 1.0]
    sc.keyValue = [[0.82, 0.82], [1.0, 1.0]]
    sc.fieldToInterp = "logo_node.scale"
    m.logo.id = "logo_node"
    op = m.anim.createChild("FloatFieldInterpolator")
    op.key = [0.0, 1.0]
    op.keyValue = [0.0, 1.0]
    op.fieldToInterp = "logo_node.opacity"
    m.timer = m.top.createChild("Timer")
    m.timer.duration = 1.9
    m.timer.observeField("fire", "goNext")
    m.started = false
end sub

sub onActiveChange()
    if not m.top.active or m.started then return
    m.started = true
    st = app("getState")
    if st.profiles.Count() > 0 then m.msg.text = "Hola de nuevo" else m.msg.text = "Preparando todo"
    m.anim.control = "start"
    sfx("fitotv")
    m.timer.control = "start"
end sub

sub goNext()
    st = app("getState")
    n = st.profiles.Count()
    if n = 0 then
        app("reset", {screen: "TutorialScreen"})
    else if n = 1 then
        app("startSession", {id: st.profiles[0].id})
        app("reset", {screen: "HolaScreen"})
    else
        app("reset", {screen: "ProfilesScreen"})
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    return press and key <> "back"
end function

function onKeyboardText(p as object) as dynamic
    return true
end function
