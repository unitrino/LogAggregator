from flask import Flask,render_template,request,json
import requests
app = Flask(__name__)

#Отображение сообщений цепочки
@app.route("/details",methods=['GET'])
def get_chains_detail():
	chain_id = request.args.get("details_chain")
	r = requests.get("http://localhost:8080/chains_detail",data = request.args)
	chains = []
	for i in json.loads(r.text):
		chains.append([i['messId'],i['level'],i['source_ip'],i['title'],i['data'],i['tags'].replace('{','').replace('}',''),i['timestamp']])
	return render_template('message.html', table_context = chains)

#Работа с цепочками сообщений
@app.route("/get_chains_info",methods=['GET'])
def get_chains_info():
	print request.args
	print request.args.get("id_channel")
	channel = request.args.get("id_channel")
	r = requests.get("http://localhost:8080/get_chains",data = request.args)
	data_context = []
	levels = []
	tags = []
	print r.text
	if len(json.loads(r.text)[0]) > 0:
		for i in json.loads(r.text)[0]:
			data_context.append([i['title'],i['mes_level'],i['last_appears'],i['num_messages'],i['tags'],i['chainId']])
		
		all_levels = {'info':0, 'debug':0, 'warning':0, 'error':0,'critical':0}
		checked_level = request.args.get("levels")
		for i in json.loads(r.text)[2]:
			curr_level = i['level']
			all_levels[curr_level] = i['numberOfMessages']
		for key, value in all_levels.iteritems():
			if key == checked_level:
				levels.append([key,value,1])
			else:
				levels.append([key,value,0])

		checked_tag = request.args.get("tags")
		for i in json.loads(r.text)[1]:
			tag_value = i['tags'].replace('{','').replace('}','')
			if tag_value == checked_tag:
				tags.append([tag_value,i['numberOfMessages'],1])
			else:
				tags.append([tag_value,i['numberOfMessages'],0])

		return render_template('chains.html', table_context = data_context,channel_id = channel,stat_levels = levels,stat_tags = tags)
	else:
		return render_template('empty_channel.html')

#Работа с таблицей каналов системы
@app.route("/tables",methods=['POST', 'GET'])
def post_hello():
	if request.method == 'POST':
		print "POST"
		r = requests.post("http://localhost:8080/tables",data = request.form)
		data_context = []
		print r.text
		for i in json.loads(r.text):
			data_context.append([i['channelId'],i['URL'],i['err'],i['criticals'],i['total_logs']])
		return render_template('table.html', table_context = data_context)

	else:
		print "ELSE POST"
		r = requests.post("http://localhost:8080/tables",data = request.form)
		data_context = []
		for i in json.loads(r.text):
			data_context.append([i['channelId'],i['URL'],i['err'],i['criticals'],i['total_logs']])
		return render_template('table.html', table_context = data_context)
	

if __name__ == "__main__":
    app.run(debug = True,host='localhost',port=5000)
