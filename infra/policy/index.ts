import { PolicyPack } from "@pulumi/policy";
import { policyManager } from "@pulumi/compliance-policy-manager";

new PolicyPack("zenobase-compliance", {
	policies: policyManager.selectPolicies({ vendors: ["aws"] }, "mandatory"),
});

policyManager.displaySelectionStats({
	displayGeneralStats: true,
	displayModuleInformation: true,
	displaySelectedPolicyNames: false,
});
