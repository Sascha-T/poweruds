(Echo open_session
Echo change_com_line^|17
Echo bind_protocol^|0310E8
Echo write_and_read^|06520752^|1003^|0) > tmp.txt
type tmp.txt | vciproxy