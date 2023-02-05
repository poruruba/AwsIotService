'use strict';

const vConsole = new VConsole();
//const remoteConsole = new RemoteConsole("http://[remote server]/logio-post");
//window.datgui = new dat.GUI();

var vue_options = {
    el: "#top",
    mixins: [mixins_bootstrap],
    store: vue_store,
    data: {
        publish_message: "",
    },
    computed: {
    },
    methods: {
        do_reload: function(){
            location.reload();
        },
        do_bind: async function(){
            try{
                await sampleaidl.bind()
                alert("bind ok");
                await sampleaidl.addListener(true, (topic, message) =>{
                    alert("topic=" + topic + " message=" + message);
                });
            }catch(error){
                alert(error);
            }
        },
        do_unbind: async function(){
            try{
                await sampleaidl.unbind()
            }catch(error){
                alert(error);
            }
        },
        do_publish: async function(){
            try{
                await sampleaidl.publishMessage(null, this.publish_message);
            }catch(error){
                alert(error);
            }
        },
    },
    created: function(){
    },
    mounted: function(){
        proc_load();
    }
};
vue_add_data(vue_options, { progress_title: '' }); // for progress-dialog
vue_add_global_components(components_bootstrap);
vue_add_global_components(components_utils);

/* add additional components */
  
window.vue = new Vue( vue_options );
