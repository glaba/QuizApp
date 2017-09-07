var net = require("net");
var fs = require("fs");
var sockets = [];

var ALL_LISTS = {};

var lists = [];
var lobbies = [];
var users = [];
var leaders = [];
var lobbyStarted = [];
var information = [];

var findLobbyFromSocket = function(socket) {
    for (var i = 0; i < users.length; i++) {
        for (var j = 0; j < users[i].length; j++) {
            if (users[i][j].socket == socket) {
                return i;
            }
        }
        if (leaders[i] && leaders[i].socket && leaders[i].socket == socket) {
            return i;
        }
    }
    return -1;
};

var sendListNames = function(sock) {
    fs.readdir(__dirname + "/lists/", function(error, items) {
        sock.write(JSON.stringify({
            what: "list_names",
            list_names: items
        }) + "\n");
    });
};

var loadLists = function() {
    fs.readdir(__dirname + "/lists/", function(error, items) {
        console.log(error);
        for (var i = 0; i < items.length; i++) {
            fs.readFile(__dirname + "/lists/" + items[i], function(index) { 
                return function(error, data) {
                    ALL_LISTS[items[index]] = JSON.parse(data);
                    console.log("\"" + items[index] + "\"");
                };
            }(i));
        }
    });
};
loadLists();

var getList = function(name) {
    // load from a file somehow a json object with the format
    // [{question: String, options: [String, ...], answerIndex: Number, time: Number}, ...]
    var curData = ALL_LISTS[name];
    if (curData.questionList) {
        // Traditional question list with question, 4 options, answerIndex, and time
        return curData.questionList;
    } else {
        // Question pool with fixed questions and their corresponding answer, and time for each question
        // We must construct the options and the answerindex
        var pool = curData.answers;
        var questions = curData.questions;
        var times = curData.times;

        var finalList = [];
        for (var i = 0; i < questions.length; i++) {
            var obj = {question: questions[i], answerIndex: 0, time: times[i]};
            var options = [pool[i]];
            var usedIndices = [i];
            for (var j = 0; j < 3; j++) {
                var curIndex = Math.floor(Math.random() * pool.length);
                while (usedIndices.indexOf(curIndex) >= 0) {
                    curIndex = Math.floor(Math.random() * pool.length);
                }
                usedIndices.push(curIndex);
                options.push(pool[curIndex]);
            }
            obj.options = options;
            finalList.push(obj);
        }
        return finalList;
    }
};

var addUserToLobby = function(index, name, socket) {
    if (index >= 0) {
        users[index].push({name: name, socket: socket});
        sendNewLobbyInformation(index);
    }
};

var sendNewLobbyInformation = function(index) {
    var userNames = [];
    for (var i = 0; i < users[index].length; i++) {
        userNames.push(users[index][i].name);
    }
    var leaderPlays = false;
    for (var i = 0; i < users[index].length; i++) {
        if (users[index][i].socket == leaders[index].socket)
            leaderPlays = true;

        setTimeout(function(ind) {
            return function() {
                users[index][ind].socket.write(JSON.stringify({
                    what: "user_list",
                    user_list: userNames
                }) + "\n");
            }
        }(i), 100);
    }
    if (!leaderPlays) {
        setTimeout(function(ind) {
            return function() {
                leaders[index].socket.write(JSON.stringify({
                    what: "user_list",
                    user_list: userNames
                }) + "\n");
            }
        }(i), 100);
    }
};

var isLeaderPlaying = function(index) {
    for (var i = 0; i < users[index].length; i++) {
        if (users[index][i].socket == leaders[index].socket)
            return true;
    }
    return false;
};

var killLobby = function(lobbyIndex, message) {
    // Send kick requests to everyone in the game
    if (message != "") {
        for (var i = 0; i < users[lobbyIndex].length; i++) {
            users[lobbyIndex][i].socket.write(JSON.stringify({
                what: "kick",
                msg: message
            }) + "\n");
        }
    }

    // Delete the game
    lobbies.splice(lobbyIndex, 1);
    leaders.splice(lobbyIndex, 1);
    lobbyStarted.splice(lobbyIndex, 1);
    information.splice(lobbyIndex, 1);
    users.splice(lobbyIndex, 1);
    lists.splice(lobbyIndex, 1);
};

var removeUserFromLobby = function(sock) {
    var lobbyIndex = findLobbyFromSocket(sock);
    if (lobbyIndex != -1) {
        for (var i = 0; i < users[lobbyIndex].length; i++) {
            if (users[lobbyIndex][i].socket == sock) {
                users[lobbyIndex].splice(i, 1);
                break;
            }
        }
        if (leaders[lobbyIndex].socket == sock) {
            killLobby(lobbyIndex, "Game leader has left!");
        } else {
            sendNewLobbyInformation(lobbyIndex);
        }
    }
};

var sendLobbyUpdate = function(mySock) {
    if (mySock) {
        mySock.write(JSON.stringify({
            what: "lobby_update",
            lobby_list: lobbies
        }) + "\n");
    } else {
        for (var i = 0; i < sockets.length; i++) {
            sockets[i].write(JSON.stringify({
                what: "lobby_update",
                lobby_list: lobbies
            }) + "\n");
        }
    }
};

var sendQuestionToLobby = function(index, questionObject, questionIndex) {
    // questionObject is an object containing a String question and an Array of Strings options
    for (var i = 0; i < users[index].length; i++) {
        console.log("Send question");
        users[index][i].socket.write(JSON.stringify({
            what: "question",
            question: questionObject
        }) + "\n");

        if (!isLeaderPlaying(index)) {
            leaders[index].socket.write(JSON.stringify({
                what: "question",
                question: questionObject
            }) + "\n");
        }
    }
};

var proceedToNextGameState = function(index) {
    // Check if the quiz is over
    var gameOver = false;
    if (lists[index].length <= users[index][0].answers.length) {
        gameOver = true;
    }

    if (information[index].showStatsEveryRound || gameOver) {
        // Compile scores
        var totalScores = [];
        for (var i = 0; i < users[index].length; i++) {
            var curScore = 0;
            for (var j = 0; j < users[index][i].answers.length; j++) {
                curScore += users[index][i].answers[j].marginalScore;
            }
            totalScores.push({name: users[index][i].name, score: curScore});
        }

        totalScores.sort(function(a, b) {
            return b.score - a.score;
        });

        // Send scores
        var allResponses = [];
        for (var i = 0; i < users[index].length; i++) {
            allResponses.push({
                name: users[index][i].name,
                answers: users[index][i].answers
            });
        }

        var scoreString = JSON.stringify({what: (gameOver ? "game_over" : "scores"),
                                          scores: totalScores,
                                          questions: lists[index],
                                          responses: allResponses // response, correct, marginalScore
                                         }) + "\n";

        var leader = leaders[index].socket;

        var curUsers = [];
        for (var i = 0; i < users[index].length; i++) {
            if (leaders[index].socket != users[index][i].socket)
                curUsers.push(users[index][i].socket);
        }

        setTimeout(function() {
            for (var i = 0; i < curUsers.length; i++) {
                curUsers[i].write(scoreString);
            }

            leader.write(scoreString);     
            sendLobbyUpdate();
        }, 1000);
    }

    // Kill game if it's over, else wait for the next command from the leader or give the next question depending on the options 
    if (gameOver) {
        killLobby(index, "");
    } else if (information[index].giveNextQuestionImmediately) {
        // Send next question 
        console.log("Wait for next question");
        setTimeout(function() {
            console.log("Send next question");
            var question = JSON.parse(JSON.stringify(lists[index][++information[index].curQuestion]));
            delete question.answerIndex;
            question.options.sort(function(a, b) {
                return Math.random() * 2 - 1;
            });
            sendQuestionToLobby(index, question, information[index].curQuestion);
        }, 1000);
    } else {
        information[index].waitingForNextQuestion = true;
        information[index].curQuestion += 0.5;
        leaders[index].socket.write(JSON.stringify({
            what: "next_question"
        }) + "\n");
        console.log("Received all answers: " + information[index].curQuestion);
    }
};

var server = net.createServer(function(sock) {
    console.log("Connected: " + sock.remoteAddress + ":" + sock.remotePort);
    sockets.push(sock);

    sendLobbyUpdate(sock);
 
    sock.on("data", function(data) {
        var json = JSON.parse(data.toString("utf-8"));

        if (json.what == "request_lobbies") {
            sendLobbyUpdate(sock);
        } else if (json.what == "join") {
            // lobby, name, password
            if (findLobbyFromSocket(sock) == -1 && lobbies.indexOf(json.lobby) >= 0 && information[lobbies.indexOf(json.lobby)].password == json.password) {
                sock.write(JSON.stringify({
                    what: "joined_lobby"
                }) + "\n");
                addUserToLobby(lobbies.indexOf(json.lobby), json.name, sock);
            } else {
                sock.write(JSON.stringify({
                    what: "wrong_password"
                }) + "\n");
            }
        } else if (json.what == "get_list_names") {
            sendListNames(sock);
        } else if (json.what == "create") {
            // gameName, leaderName, leaderPlays, listName, randomizeOrder, showStatsEveryRound, password, giveNextQuestionImmediately
            if (findLobbyFromSocket(sock) == -1) {
                lobbies.push(json.gameName + ((json.password != "") ? " 🔒" : ""));
                users.push([]);
                leaders.push({name: json.leaderName, socket: sock});

                if (json.leaderPlays) {
                    addUserToLobby(lobbies.length - 1, json.leaderName, sock);
                }                    

                lobbyStarted.push(false);
                
                var curList = getList(json.listName);
                if (json.randomizeOrder) {
                    curList.sort(function(a, b) {
                        return Math.random() * 2 - 1;
                    });
                    console.log(curList);
                }
                lists.push(curList);

                information.push({showStatsEveryRound: json.showStatsEveryRound, password: json.password, giveNextQuestionImmediately: json.giveNextQuestionImmediately});

                sendLobbyUpdate();
            }
        } else if (json.what == "kick_user") {
            // user
            // Check if the person requesting the kick is a leader of a lobby
            for (var i = 0; i < leaders.length; i++) {
                if (sock == leaders[i].socket) {
                    for (var j = 0; j < users[i].length; j++) {
                        if (users[i][j].name == json.user && leaders[i].name != json.user) {
                            users[i][j].socket.write(JSON.stringify({
                                what: "kick",
                                msg: "You have been kicked!"
                            }) + "\n");
                            users[i].splice(j, 1);
                            sendNewLobbyInformation(i);
                            break;
                        }
                    }
                    break;
                }
            }
        } else if (json.what == "leave_lobby") {
            removeUserFromLobby(sock);
            sendLobbyUpdate();
        } else if (json.what == "start_game") {
            sendLobbyUpdate();
            for (var i = 0; i < leaders.length; i++) {
                if (leaders[i].socket == sock) {
                    lobbyStarted[i] = true;
                    information[i].curQuestion = 0;
                    for (var j = 0; j < users[i].length; j++) {
                        users[i][j].socket.write(JSON.stringify({
                            what: "start_game"
                        }) + "\n");
                        users[i][j].gamePrepared = false;
                        users[i][j].answers = [];
                    }

                    if (!isLeaderPlaying(i)) {
                        leaders[i].socket.write(JSON.stringify({
                            what: "start_game_live_updates"
                        }) + "\n");
                    }

                    break;
                }
            }
        } else if (json.what == "game_prepared") {
            // Check if all other users have prepared too
            var index = findLobbyFromSocket(sock);
            if (index >= 0 && !information[index].curQuestion) {
                var allUsersPrepared = true;
                for (var i = 0; i < users[index].length; i++) {
                    if (users[index][i].socket == sock) {
                        users[index][i].gamePrepared = true;
                    }
                    allUsersPrepared = allUsersPrepared && users[index][i].gamePrepared;
                }

                if (!isLeaderPlaying(index)) {
                    if (leaders[index].socket == sock) {
                        leaders[index].gamePrepared = true;
                    }
                    allUsersPrepared = allUsersPrepared && leaders[index].gamePrepared;
                }

                if (allUsersPrepared) {
                    console.log("All users prepared");
                    // Send first question 
                    var question = JSON.parse(JSON.stringify(lists[index][0]));
                    delete question.answerIndex;
                    question.options.sort(function(a, b) {
                        return Math.random() * 2 - 1;
                    });
                    sendQuestionToLobby(index, question, information[index].curQuestion);
                    information[index].curQuestion = 0;
                }
            }
        } else if (json.what == "answer") {
            console.log("Get answer");
            // response, timeToAnswer
            var index = findLobbyFromSocket(sock);
            if (index >= 0) {
                var allUsersAnswered = true;
                for (var i = 0; i < users[index].length; i++) {
                    if (users[index][i].socket == sock && users[index][i].answers.length == information[index].curQuestion) {
                        var score = 0;
                        var curQuestion = lists[index][information[index].curQuestion];
                        var correctAnswer = false;

                        if (json.response == curQuestion.options[curQuestion.answerIndex]) {
                            score = Math.max(0, lists[index][information[index].curQuestion].time - Math.max(json.timeToAnswer, 0));
                            correctAnswer = true;
                        }

                        users[index][i].answers.push({
                            response: json.response,
                            correct: correctAnswer,
                            marginalScore: score
                        });

                        if (!isLeaderPlaying(index)) {
                            // Send live updates
                            leaders[index].socket.write(JSON.stringify({
                                what: "live_update",
                                user: users[index][i].name,
                                response: json.response,
                                correctAnswer: correctAnswer,
                                time: json.timeToAnswer
                            }) + "\n");
                        }
                    }
                    if (users[index][i].answers.length <= information[index].curQuestion) {
                        allUsersAnswered = false;
                    }
                }

                if (allUsersAnswered) {
                    proceedToNextGameState(index);
                }
            }
        } else if (json.what == "next_question") {
            for (var i = 0; i < leaders.length; i++) {
                if (sock == leaders[i].socket) {
                    if (information[i].waitingForNextQuestion) {
                        information[i].waitingForNextQuestion = false;

                        // Send next question 
                        information[i].curQuestion = information[i].curQuestion + 0.5;
                        var question = JSON.parse(JSON.stringify(lists[i][information[i].curQuestion]));
                        delete question.answerIndex;
                        question.options.sort(function(a, b) {
                            return Math.random() * 2 - 1;
                        });
                        sendQuestionToLobby(i, question, information[i].curQuestion);
                    }
                    break;
                }
            }
        }
    });
 
    sock.on("end", function() {
        console.log("Disconnected: " + sock.remoteAddress + ":" + sock.remotePort);

        removeUserFromLobby(sock);

        var index = sockets.indexOf(sock);
        if (index != -1) {
            sockets.splice(index, 1);
        }
    });
});
 
var serverAddress = "107.170.2.182";
var serverPort = 1234;

server.listen(serverPort, serverAddress);
console.log("Server Created at " + serverAddress + ":" + serverPort + "\n");