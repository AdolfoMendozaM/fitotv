sub Main(args as dynamic)
    screen = CreateObject("roSGScreen")
    port = CreateObject("roMessagePort")
    screen.SetMessagePort(port)
    m.global = screen.getGlobalNode()
    m.global.addField("content", "node", false)
    m.global.addFields({kids: false, sound: true})
    sc = screen.CreateScene("MainScene")
    screen.Show()
    sc.observeField("exitApp", port)
    while true
        msg = wait(0, port)
        t = type(msg)
        if t = "roSGScreenEvent" then
            if msg.IsScreenClosed() then return
        else if t = "roSGNodeEvent" then
            if msg.getField() = "exitApp" and msg.getData() = true then return
        end if
    end while
end sub
