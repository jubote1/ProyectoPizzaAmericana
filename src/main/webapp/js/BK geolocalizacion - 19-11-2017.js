

          

function ubicarTienda(latitud, longitud , mapa) {

     var manriquecor = [
          {lat: 6.292944,lng: -75.561688},
          {lat: 6.292124,lng: -75.561902},
          {lat: 6.291158,lng: -75.562205},
          {lat: 6.289566,lng: -75.563509},
          {lat: 6.28865,lng: -75.563974},
          {lat: 6.287735,lng: -75.5643},
          {lat: 6.286724,lng: -75.564094},
          {lat: 6.286206,lng: -75.564155},
          {lat: 6.285699,lng: -75.564024},
          {lat: 6.285416,lng: -75.564348},
          {lat: 6.284941,lng: -75.564501},
          {lat: 6.284534,lng: -75.565022},
          {lat: 6.28385,lng: -75.565059},
          {lat: 6.283064,lng: -75.565419},
          {lat: 6.282672,lng: -75.565971},
          {lat: 6.282071,lng: -75.566251},
          {lat: 6.281471,lng: -75.566392},
          {lat: 6.281197,lng: -75.566298},
          {lat: 6.280957,lng: -75.566557},
          {lat: 6.280394,lng: -75.566215},
          {lat: 6.280055,lng: -75.566644},
          {lat: 6.279468,lng: -75.566416},
          {lat: 6.278891,lng: -75.56607},
          {lat: 6.277971,lng: -75.565796},
          {lat: 6.277105,lng: -75.565349},
          {lat: 6.276782,lng: -75.565931},
          {lat: 6.276342,lng: -75.565963},
          {lat: 6.276353,lng: -75.566562},
          {lat: 6.276225,lng: -75.567193},
          {lat: 6.276438,lng: -75.567776},
          {lat: 6.276598,lng: -75.568552},
          {lat: 6.275791,lng: -75.568936},
          {lat: 6.27091,lng: -75.570243},
          {lat: 6.265179,lng: -75.571633},
          {lat: 6.262228,lng: -75.572511},
          {lat: 6.258979,lng: -75.571758},
          {lat: 6.258696,lng: -75.571287},
          {lat: 6.258237,lng: -75.571798},
          {lat: 6.257638,lng: -75.570997},
          {lat: 6.257039,lng: -75.571648},
          {lat: 6.256387,lng: -75.571476},
          {lat: 6.255934,lng: -75.571284},
          {lat: 6.255487,lng: -75.571209},
          {lat: 6.254683,lng: -75.571286},
          {lat: 6.254136,lng: -75.57116},
          {lat: 6.253762,lng: -75.570941},
          {lat: 6.253374,lng: -75.570167},
          {lat: 6.25305,lng: -75.569349},
          {lat: 6.252475,lng: -75.567771},
          {lat: 6.251847,lng: -75.567794},
          {lat: 6.251198,lng: -75.567979},
          {lat: 6.2499,lng: -75.568471},
          {lat: 6.248692,lng: -75.569082},
          {lat: 6.247473,lng: -75.569643},
          {lat: 6.246068,lng: -75.570168},
          {lat: 6.244694,lng: -75.570689},
          {lat: 6.244068,lng: -75.569096},
          {lat: 6.243537,lng: -75.5675},
          {lat: 6.242879,lng: -75.5657},
          {lat: 6.242228,lng: -75.564163},
          {lat: 6.24162,lng: -75.563265},
          {lat: 6.241951,lng: -75.562651},
          {lat: 6.242221,lng: -75.562408},
          {lat: 6.242413,lng: -75.561947},
          {lat: 6.242403,lng: -75.561513},
          {lat: 6.242246,lng: -75.561079},
          {lat: 6.242009,lng: -75.560747},
          {lat: 6.24198,lng: -75.560412},
          {lat: 6.242081,lng: -75.560135},
          {lat: 6.24134,lng: -75.559413},
          {lat: 6.24096,lng: -75.55922},
          {lat: 6.240396,lng: -75.558681},
          {lat: 6.239315,lng: -75.557088},
          {lat: 6.239052,lng: -75.556438},
          {lat: 6.238397,lng: -75.556798},
          {lat: 6.23771,lng: -75.555529},
          {lat: 6.237399,lng: -75.554758},
          {lat: 6.236976,lng: -75.554008},
          {lat: 6.237247,lng: -75.553708},
          {lat: 6.236947,lng: -75.553457},
          {lat: 6.236642,lng: -75.553249},
          {lat: 6.236365,lng: -75.552757},
          {lat: 6.236184,lng: -75.552254},
          {lat: 6.237165,lng: -75.551777},
          {lat: 6.237154,lng: -75.551678},
          {lat: 6.238194,lng: -75.551112},
          {lat: 6.238412,lng: -75.551129},
          {lat: 6.238932,lng: -75.550937},
          {lat: 6.239666,lng: -75.551316},
          {lat: 6.240199,lng: -75.551256},
          {lat: 6.24084,lng: -75.551011},
          {lat: 6.241496,lng: -75.550619},
          {lat: 6.242542,lng: -75.550098},
          {lat: 6.242759,lng: -75.550106},
          {lat: 6.242722,lng: -75.551125},
          {lat: 6.242888,lng: -75.552592},
          {lat: 6.243364,lng: -75.552308},
          {lat: 6.244407,lng: -75.551645},
          {lat: 6.24546,lng: -75.551553},
          {lat: 6.245936,lng: -75.551321},
          {lat: 6.246365,lng: -75.551339},
          {lat: 6.246877,lng: -75.552198},
          {lat: 6.247214,lng: -75.550124},
          {lat: 6.24719,lng: -75.548549},
          {lat: 6.248942,lng: -75.548527},
          {lat: 6.248978,lng: -75.548815},
          {lat: 6.249637,lng: -75.548753},
          {lat: 6.249428,lng: -75.549493},
          {lat: 6.249211,lng: -75.550122},
          {lat: 6.249096,lng: -75.551389},
          {lat: 6.248914,lng: -75.552228},
          {lat: 6.248971,lng: -75.552662},
          {lat: 6.249624,lng: -75.552195},
          {lat: 6.249974,lng: -75.552056},
          {lat: 6.25036,lng: -75.552002},
          {lat: 6.250493,lng: -75.552029},
          {lat: 6.250435,lng: -75.552344},
          {lat: 6.250508,lng: -75.552748},
          {lat: 6.250764,lng: -75.553122},
          {lat: 6.250803,lng: -75.553695},
          {lat: 6.250884,lng: -75.554021},
          {lat: 6.25066,lng: -75.554714},
          {lat: 6.250647,lng: -75.555071},
          {lat: 6.251849,lng: -75.554768},
          {lat: 6.253433,lng: -75.55369},
          {lat: 6.253718,lng: -75.553032},
          {lat: 6.253847,lng: -75.552539},
          {lat: 6.254697,lng: -75.551058},
          {lat: 6.254501,lng: -75.550897},
          {lat: 6.254402,lng: -75.55064},
          {lat: 6.254789,lng: -75.550184},
          {lat: 6.255041,lng: -75.550165},
          {lat: 6.255278,lng: -75.550205},
          {lat: 6.255548,lng: -75.549387},
          {lat: 6.256235,lng: -75.548984},
          {lat: 6.256727,lng: -75.54967},
          {lat: 6.258056,lng: -75.548792},
          {lat: 6.258271,lng: -75.548977},
          {lat: 6.258785,lng: -75.548604},
          {lat: 6.25896,lng: -75.548441},
          {lat: 6.260463,lng: -75.5475},
          {lat: 6.261063,lng: -75.547633},
          {lat: 6.261098,lng: -75.547173},
          {lat: 6.262094,lng: -75.546919},
          {lat: 6.262299,lng: -75.546018},
          {lat: 6.262198,lng: -75.545837},
          {lat: 6.261648,lng: -75.544968},
          {lat: 6.261561,lng: -75.544707},
          {lat: 6.261996,lng: -75.544702},
          {lat: 6.262533,lng: -75.544939},
          {lat: 6.262918,lng: -75.544754},
          {lat: 6.263359,lng: -75.544758},
          {lat: 6.263949,lng: -75.544662},
          {lat: 6.264262,lng: -75.544446},
          {lat: 6.264503,lng: -75.544473},
          {lat: 6.265013,lng: -75.544744},
          {lat: 6.265206,lng: -75.544645},
          {lat: 6.265854,lng: -75.54438},
          {lat: 6.266231,lng: -75.544562},
          {lat: 6.266531,lng: -75.544577},
          {lat: 6.26677,lng: -75.544487},
          {lat: 6.26698,lng: -75.544355},
          {lat: 6.267134,lng: -75.544309},
          {lat: 6.26736,lng: -75.544271},
          {lat: 6.267571,lng: -75.544236},
          {lat: 6.267848,lng: -75.544441},
          {lat: 6.268184,lng: -75.544715},
          {lat: 6.268959,lng: -75.544893},
          {lat: 6.269342,lng: -75.545155},
          {lat: 6.270779,lng: -75.545223},
          {lat: 6.271501,lng: -75.545575},
          {lat: 6.272269,lng: -75.545819},
          {lat: 6.272829,lng: -75.545695},
          {lat: 6.273199,lng: -75.545707},
          {lat: 6.273555,lng: -75.545993},
          {lat: 6.273826,lng: -75.546364},
          {lat: 6.274441,lng: -75.546478},
          {lat: 6.275148,lng: -75.546469},
          {lat: 6.275041,lng: -75.545873},
          {lat: 6.275679,lng: -75.545867},
          {lat: 6.276215,lng: -75.545769},
          {lat: 6.27654,lng: -75.545796},
          {lat: 6.276564,lng: -75.54592},
          {lat: 6.277015,lng: -75.545909},
          {lat: 6.279122,lng: -75.545826},
          {lat: 6.27982,lng: -75.545635},
          {lat: 6.280345,lng: -75.545483},
          {lat: 6.281037,lng: -75.545324},
          {lat: 6.282037,lng: -75.545455},
          {lat: 6.28249,lng: -75.545447},
          {lat: 6.282899,lng: -75.54558},
          {lat: 6.28317,lng: -75.545796},
          {lat: 6.283422,lng: -75.545676},
          {lat: 6.283682,lng: -75.545749},
          {lat: 6.284056,lng: -75.545943},
          {lat: 6.284204,lng: -75.545875},
          {lat: 6.284291,lng: -75.545476},
          {lat: 6.284369,lng: -75.544986},
          {lat: 6.284605,lng: -75.5446},
          {lat: 6.284806,lng: -75.544487},
          {lat: 6.285062,lng: -75.544502},
          {lat: 6.285511,lng: -75.544485},
          {lat: 6.285946,lng: -75.544204},
          {lat: 6.286412,lng: -75.544152},
          {lat: 6.286832,lng: -75.544126},
          {lat: 6.287484,lng: -75.544237},
          {lat: 6.287701,lng: -75.544569},
          {lat: 6.288066,lng: -75.544906},
          {lat: 6.288508,lng: -75.545279},
          {lat: 6.288985,lng: -75.547055},
          {lat: 6.289838,lng: -75.546852},
          {lat: 6.28983,lng: -75.549445},
          {lat: 6.290577,lng: -75.552093},
          {lat: 6.291774,lng: -75.55586},
          {lat: 6.292096,lng: -75.557747},
          {lat: 6.292088,lng: -75.558087},
          {lat: 6.292479,lng: -75.559784},
          {lat: 6.292944,lng: -75.561688}
];

     var pobladocor = [
          {lat: 6.211125,lng: -75.541374},
          {lat: 6.213434,lng: -75.541978},
          {lat: 6.213597,lng: -75.542596},
          {lat: 6.21475,lng: -75.542691},
          {lat: 6.21497,lng: -75.54299},
          {lat: 6.215861,lng: -75.542576},
          {lat: 6.216082,lng: -75.542929},
          {lat: 6.216527,lng: -75.543067},
          {lat: 6.217332,lng: -75.544234},
          {lat: 6.21758,lng: -75.545606},
          {lat: 6.218778,lng: -75.546828},
          {lat: 6.22101,lng: -75.550729},
          {lat: 6.219134,lng: -75.552948},
          {lat: 6.216124,lng: -75.552746},
          {lat: 6.222072,lng: -75.559552},
          {lat: 6.222965,lng: -75.559596},
          {lat: 6.226631,lng: -75.558964},
          {lat: 6.226289,lng: -75.557608},
          {lat: 6.227059,lng: -75.556638},
          {lat: 6.227811,lng: -75.556598},
          {lat: 6.227926,lng: -75.556849},
          {lat: 6.228138,lng: -75.556988},
          {lat: 6.228829,lng: -75.556858},
          {lat: 6.230357,lng: -75.557061},
          {lat: 6.23166,lng: -75.55797},
          {lat: 6.231826,lng: -75.558219},
          {lat: 6.231853,lng: -75.558757},
          {lat: 6.232159,lng: -75.559095},
          {lat: 6.232151,lng: -75.559393},
          {lat: 6.232424,lng: -75.560062},
          {lat: 6.232726,lng: -75.560297},
          {lat: 6.232825,lng: -75.560607},
          {lat: 6.233619,lng: -75.560231},
          {lat: 6.234824,lng: -75.560127},
          {lat: 6.235632,lng: -75.559726},
          {lat: 6.235611,lng: -75.560907},
          {lat: 6.235882,lng: -75.560953},
          {lat: 6.236195,lng: -75.561239},
          {lat: 6.236519,lng: -75.56158},
          {lat: 6.237145,lng: -75.562276},
          {lat: 6.237848,lng: -75.561594},
          {lat: 6.238023,lng: -75.561755},
          {lat: 6.23865,lng: -75.56146},
          {lat: 6.239075,lng: -75.561382},
          {lat: 6.239421,lng: -75.561294},
          {lat: 6.239495,lng: -75.561119},
          {lat: 6.239718,lng: -75.560869},
          {lat: 6.240661,lng: -75.560784},
          {lat: 6.242,lng: -75.560745},
          {lat: 6.242158,lng: -75.560908},
          {lat: 6.242368,lng: -75.561278},
          {lat: 6.242406,lng: -75.561777},
          {lat: 6.242362,lng: -75.562076},
          {lat: 6.242186,lng: -75.562455},
          {lat: 6.241925,lng: -75.562678},
          {lat: 6.241611,lng: -75.563243},
          {lat: 6.24199,lng: -75.563787},
          {lat: 6.242358,lng: -75.564417},
          {lat: 6.24295,lng: -75.565816},
          {lat: 6.243868,lng: -75.568511},
          {lat: 6.23913,lng: -75.570437},
          {lat: 6.235611,lng: -75.570216},
          {lat: 6.227769,lng: -75.56957},
          {lat: 6.230859,lng: -75.573926},
          {lat: 6.231966,lng: -75.577574},
          {lat: 6.231752,lng: -75.587338},
          {lat: 6.229094,lng: -75.587267},
          {lat: 6.226499,lng: -75.58756},
          {lat: 6.221716,lng: -75.588706},
          {lat: 6.216559,lng: -75.589615},
          {lat: 6.214564,lng: -75.590552},
          {lat: 6.212506,lng: -75.590889},
          {lat: 6.210594,lng: -75.591412},
          {lat: 6.208077,lng: -75.591752},
          {lat: 6.207994,lng: -75.590606},
          {lat: 6.207945,lng: -75.589953},
          {lat: 6.207688,lng: -75.589558},
          {lat: 6.205896,lng: -75.589085},
          {lat: 6.204946,lng: -75.588716},
          {lat: 6.204187,lng: -75.588855},
          {lat: 6.203551,lng: -75.589251},
          {lat: 6.203347,lng: -75.589732},
          {lat: 6.202775,lng: -75.590085},
          {lat: 6.202549,lng: -75.590484},
          {lat: 6.201976,lng: -75.59052},
          {lat: 6.201429,lng: -75.590486},
          {lat: 6.200947,lng: -75.588555},
          {lat: 6.197686,lng: -75.589808},
          {lat: 6.195147,lng: -75.581163},
          {lat: 6.191779,lng: -75.5823},
          {lat: 6.188475,lng: -75.583019},
          {lat: 6.188225,lng: -75.582751},
          {lat: 6.187782,lng: -75.581485},
          {lat: 6.181441,lng: -75.587016},
          {lat: 6.17935,lng: -75.585055},
          {lat: 6.17966,lng: -75.584779},
          {lat: 6.178084,lng: -75.582245},
          {lat: 6.176294,lng: -75.579755},
          {lat: 6.172769,lng: -75.574881},
          {lat: 6.171734,lng: -75.574294},
          {lat: 6.171254,lng: -75.573278},
          {lat: 6.170742,lng: -75.573182},
          {lat: 6.17023,lng: -75.572894},
          {lat: 6.169782,lng: -75.572198},
          {lat: 6.169206,lng: -75.571802},
          {lat: 6.169888,lng: -75.57031},
          {lat: 6.169972,lng: -75.568903},
          {lat: 6.169137,lng: -75.565531},
          {lat: 6.167449,lng: -75.562267},
          {lat: 6.170614,lng: -75.560955},
          {lat: 6.170846,lng: -75.561532},
          {lat: 6.171148,lng: -75.562093},
          {lat: 6.17139,lng: -75.56217},
          {lat: 6.171938,lng: -75.562081},
          {lat: 6.172071,lng: -75.562103},
          {lat: 6.172297,lng: -75.562203},
          {lat: 6.173069,lng: -75.562161},
          {lat: 6.173852,lng: -75.56191},
          {lat: 6.174406,lng: -75.56136},
          {lat: 6.174548,lng: -75.561155},
          {lat: 6.175052,lng: -75.561108},
          {lat: 6.175523,lng: -75.561198},
          {lat: 6.176,lng: -75.56123},
          {lat: 6.176532,lng: -75.561468},
          {lat: 6.177174,lng: -75.561378},
          {lat: 6.177833,lng: -75.561116},
          {lat: 6.178128,lng: -75.56159},
          {lat: 6.178782,lng: -75.561765},
          {lat: 6.179222,lng: -75.562095},
          {lat: 6.179654,lng: -75.561621},
          {lat: 6.180164,lng: -75.561241},
          {lat: 6.180096,lng: -75.560815},
          {lat: 6.180294,lng: -75.560228},
          {lat: 6.180718,lng: -75.559188},
          {lat: 6.18091,lng: -75.558686},
          {lat: 6.181278,lng: -75.558394},
          {lat: 6.181905,lng: -75.558339},
          {lat: 6.182203,lng: -75.558509},
          {lat: 6.182756,lng: -75.558573},
          {lat: 6.184256,lng: -75.558682},
          {lat: 6.185064,lng: -75.558723},
          {lat: 6.18591,lng: -75.558885},
          {lat: 6.186849,lng: -75.559067},
          {lat: 6.186645,lng: -75.558593},
          {lat: 6.186227,lng: -75.55784},
          {lat: 6.185725,lng: -75.557242},
          {lat: 6.185241,lng: -75.55664},
          {lat: 6.185264,lng: -75.555861},
          {lat: 6.185,lng: -75.555163},
          {lat: 6.18519,lng: -75.554634},
          {lat: 6.185162,lng: -75.553964},
          {lat: 6.185125,lng: -75.553354},
          {lat: 6.185051,lng: -75.552759},
          {lat: 6.18478,lng: -75.552415},
          {lat: 6.184402,lng: -75.552258},
          {lat: 6.183497,lng: -75.551898},
          {lat: 6.182921,lng: -75.551191},
          {lat: 6.182473,lng: -75.551036},
          {lat: 6.182196,lng: -75.550747},
          {lat: 6.182129,lng: -75.550488},
          {lat: 6.182238,lng: -75.550294},
          {lat: 6.182498,lng: -75.550104},
          {lat: 6.183015,lng: -75.550006},
          {lat: 6.184048,lng: -75.549928},
          {lat: 6.185229,lng: -75.549195},
          {lat: 6.186236,lng: -75.54875},
          {lat: 6.186337,lng: -75.548464},
          {lat: 6.186315,lng: -75.548167},
          {lat: 6.186164,lng: -75.547906},
          {lat: 6.185901,lng: -75.547694},
          {lat: 6.184793,lng: -75.547564},
          {lat: 6.183538,lng: -75.547355},
          {lat: 6.183297,lng: -75.546855},
          {lat: 6.183564,lng: -75.546463},
          {lat: 6.184017,lng: -75.546382},
          {lat: 6.184702,lng: -75.54608},
          {lat: 6.184726,lng: -75.545629},
          {lat: 6.18513,lng: -75.545664},
          {lat: 6.185402,lng: -75.546207},
          {lat: 6.185897,lng: -75.546557},
          {lat: 6.187159,lng: -75.546528},
          {lat: 6.189862,lng: -75.547442},
          {lat: 6.191459,lng: -75.547311},
          {lat: 6.193056,lng: -75.546879},
          {lat: 6.1939,lng: -75.547174},
          {lat: 6.194894,lng: -75.54734},
          {lat: 6.195583,lng: -75.547029},
          {lat: 6.196283,lng: -75.54689},
          {lat: 6.197987,lng: -75.547255},
          {lat: 6.19871,lng: -75.547001},
          {lat: 6.199284,lng: -75.546469},
          {lat: 6.200146,lng: -75.546634},
          {lat: 6.200454,lng: -75.54631},
          {lat: 6.200216,lng: -75.545865},
          {lat: 6.200064,lng: -75.545377},
          {lat: 6.200339,lng: -75.54483},
          {lat: 6.200816,lng: -75.544615},
          {lat: 6.201963,lng: -75.544437},
          {lat: 6.203111,lng: -75.544162},
          {lat: 6.204029,lng: -75.544552},
          {lat: 6.205,lng: -75.54454},
          {lat: 6.205939,lng: -75.545125},
          {lat: 6.20762,lng: -75.545688},
          {lat: 6.208506,lng: -75.5456},
          {lat: 6.209306,lng: -75.545915},
          {lat: 6.210181,lng: -75.545812},
          {lat: 6.211125,lng: -75.541374}
                  ];

     var coordenada = new google.maps.LatLng(latitud, longitud);
     //validamos poblado
     var poligono1 = new google.maps.Polygon(pobladocor);
     //otra prueba
     poligono1 = new google.maps.Polygon({paths: pobladocor,
            strokeColor: "#FF0000",
            strokeOpacity: 0.8,
            strokeWeight: 2,
            fillColor: "#FF0000",
            fillOpacity: 0.35
        });
     poligono1.setMap(mapa);
     if(google.maps.geometry.poly.containsLocation(coordenada, poligono1) == true) {
          alert("ESTA DENTRO DE COBERTURA TIENDA POBLADO");
          return;
     }
     var poligono2 = new google.maps.Polygon(manriquecor);
     poligono2.setMap(mapa);
     poligono2 = new google.maps.Polygon({paths: manriquecor,
            strokeColor: "#FF0000",
            strokeOpacity: 0.8,
            strokeWeight: 2,
            fillColor: "#FF0000",
            fillOpacity: 0.35
        });
     console.log(google.maps.geometry.poly.containsLocation(coordenada, poligono2));

     if(google.maps.geometry.poly.containsLocation(coordenada, poligono2) == true) {
          alert("ESTA DENTRO DE COBERTURA TIENDA MANRIQUE");
          return;
     }
     console.log("pase por aca 3");
          
}
