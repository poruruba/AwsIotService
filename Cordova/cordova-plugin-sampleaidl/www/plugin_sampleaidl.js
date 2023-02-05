class SampleAidl{
	constructor(){
	}

	bind(){
		return new Promise(function(resolve, reject){
			cordova.exec(
				function(result){
					resolve(result);
				},
				function(err){
					reject(err);
				},
				"SampleAidl", "bind",
				[]);
		});
	}

	unbind(){
		return new Promise(function(resolve, reject){
			cordova.exec(
				function(result){
					resolve(result);
				},
				function(err){
					reject(err);
				},
				"SampleAidl", "unbind",
				[]);
		});
	}
	
	isSubscribed(){
		return new Promise(function(resolve, reject){
			cordova.exec(
				function(result){
					resolve(result == 0);
				},
				function(err){
					reject(err);
				},
				"SampleAidl", "isSubscribed",
				[]);
		});
	}

	isBound(){
		return new Promise(function(resolve, reject){
			cordova.exec(
				function(result){
					resolve(result == 0);
				},
				function(err){
					reject(err);
				},
				"SampleAidl", "isBound",
				[]);
		});
	}

	publishMessage(topicName, message){
		return new Promise(function(resolve, reject){
			cordova.exec(
				function(result){
					resolve(result);
				},
				function(err){
					reject(err);
				},
				"SampleAidl", "publishMessage",
				[topicName, message]);
		});
	}

	addListener(enable, callback){
		cordova.exec(
			function(result){
				callback(result.topicName, result.message);
			},
			function(err){
				console.error("addListener call failed");
			},
			"SampleAidl", "addListener",
			[enable]);
	}
}

module.exports = new SampleAidl();
