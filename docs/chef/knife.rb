current_dir = File.dirname(__FILE__)
log_level                :info
log_location             STDOUT
node_name                "ejain"
client_key               "#{current_dir}/ejain.pem"
validation_client_name   "zenobase-validator"
validation_key           "#{current_dir}/zenobase-validator.pem"
chef_server_url          "https://api.opscode.com/organizations/zenobase"
cache_type               'BasicFile'
cache_options( :path => "#{ENV['HOME']}/.chef/checksums" )
cookbook_path            ["#{current_dir}/../cookbooks"]

knife[:aws_ssh_key_id] = "ejain"
knife[:aws_access_key_id]     = "AKIAI23R5FZZ4L4KPSRA"
knife[:aws_secret_access_key] = "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"
